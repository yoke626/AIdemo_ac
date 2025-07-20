package com.xiaomi.service;

import com.xiaomi.entity.ChatHistory;
import com.xiaomi.entity.ChatSession;
import com.xiaomi.entity.User;
import com.xiaomi.repository.ChatHistoryRepository;
import com.xiaomi.repository.ChatSessionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private ChatHistoryRepository historyRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ZhipuLLMService llmService;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Cache<String, String> qaCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);
    private static final int MAX_CONTEXT_TOKENS = 2048;

    public SseEmitter getStreamingResponse(String question, User user, String sessionId) {
        // 现有实现保持不变...
        Long sessionIdLong;
        ChatSession session;

        try {
            sessionIdLong = Long.parseLong(sessionId);
            Optional<ChatSession> sessionOpt = sessionRepository.findById(sessionIdLong);

            if (!sessionOpt.isPresent() || !sessionOpt.get().getUser().getId().equals(user.getId())) {
                SseEmitter emitter = new SseEmitter();
                emitter.completeWithError(new RuntimeException("无效的会话ID"));
                return emitter;
            }
            session = sessionOpt.get();
        } catch (NumberFormatException e) {
            SseEmitter emitter = new SseEmitter();
            emitter.completeWithError(new RuntimeException("无效的会话ID格式"));
            return emitter;
        }

        String cacheKey = sessionId + ":" + question.trim().toLowerCase();
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        executor.submit(() -> {
            try {
                String cachedAnswer = qaCache.getIfPresent(cacheKey);

                if (cachedAnswer != null) {
                    System.out.println("本地缓存命中: " + question);
                    saveHistory(question, cachedAnswer, user, sessionIdLong);
                    emitter.send(SseEmitter.event().data(cachedAnswer));
                    emitter.complete();
                } else {
                    System.out.println("本地缓存未命中，调用大模型: " + question);
                    final StringBuilder fullAnswer = new StringBuilder();

                    List<Map<String, String>> contextMessages = buildTokenAwareContext(user, sessionIdLong);

                    Runnable saveAndCacheCallback = () -> {
                        String finalAnswer = fullAnswer.toString().trim();
                        if (!finalAnswer.isEmpty()) {
                            System.out.println("流式输出完成，保存历史记录和缓存...");
                            saveHistory(question, finalAnswer, user, sessionIdLong);
                            qaCache.put(cacheKey, finalAnswer);
                        }
                    };

                    llmService.streamChatResponse(question, emitter, fullAnswer, contextMessages, saveAndCacheCallback);
                }

                sessionService.updateSessionWithMessage(session, question);
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // 新增：获取会话历史消息的方法
    public List<Map<String, Object>> getSessionHistory(Long sessionId) {
        // 验证会话是否存在
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));

        // 查询该会话的所有历史消息，按时间升序排列
        List<ChatHistory> histories = historyRepository.findBySessionOrderByTimestampAsc(session);

        // 转换为前端需要的格式
        return histories.stream().map(history -> {
            Map<String, Object> message = new HashMap<>();
            message.put("content", history.getQuestion());
            message.put("role", "user");
            message.put("timestamp", history.getTimestamp());

            Map<String, Object> response = new HashMap<>();
            response.put("content", history.getAnswer());
            response.put("role", "assistant");
            response.put("timestamp", history.getTimestamp());

            List<Map<String, Object>> pair = new ArrayList<>();
            pair.add(message);
            pair.add(response);
            return pair;
        }).flatMap(Collection::stream).collect(Collectors.toList());
    }

    private List<Map<String, String>> buildTokenAwareContext(User user, Long sessionId) {
        // 现有实现保持不变...
        LinkedList<Map<String, String>> context = new LinkedList<>();
        int currentTokens = 0;

        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));

        Pageable pageable = PageRequest.of(0, 20);
        List<ChatHistory> history = historyRepository.findBySessionOrderByTimestampDesc(session, pageable);

        for (ChatHistory h : history) {
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", h.getQuestion());
            int qTokens = encoding.countTokens(h.getQuestion());

            Map<String, String> assistantMessage = new HashMap<>();
            assistantMessage.put("role", "assistant");
            assistantMessage.put("content", h.getAnswer());
            int aTokens = encoding.countTokens(h.getAnswer());

            if (currentTokens + qTokens + aTokens + 500 > MAX_CONTEXT_TOKENS) {
                break;
            }

            context.addFirst(assistantMessage);
            context.addFirst(userMessage);

            currentTokens += qTokens + aTokens;
        }

        System.out.println("构建了包含 " + context.size() + " 条历史消息和 " + currentTokens + " 个Token的上下文");
        return context;
    }

    private void saveHistory(String question, String answer, User user, Long sessionId) {
        // 现有实现保持不变...
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("会话不存在: " + sessionId));
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.save(session);

        ChatHistory history = new ChatHistory();
        history.setQuestion(question);
        history.setAnswer(answer);
        history.setUser(user);
        history.setSession(session);
        history.setTimestamp(LocalDateTime.now());
        historyRepository.save(history);

        System.out.println("成功保存历史记录到数据库，会话ID: " + sessionId);
    }
}
