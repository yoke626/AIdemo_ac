package com.xiaomi.service;

import com.xiaomi.entity.ChatSession;
import com.xiaomi.entity.User;
import com.xiaomi.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessionService {

    @Autowired
    private ChatSessionRepository sessionRepository;

    /**
     * 创建新会话
     */
    public ChatSession createNewSession(User user) {
        ChatSession session = new ChatSession();
        session.setUser(user);
        session.setSessionName("新会话"); // 初始名称
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    /**
     * 获取用户的所有会话
     */
    public List<ChatSession> getUserSessions(User user) {
        return sessionRepository.findByUserOrderByUpdateTimeDesc(user);
    }

    /**
     * 更新会话最后消息和时间，并自动设置会话名称
     */
    public void updateSessionWithMessage(ChatSession session, String message) {
        if (session == null || message == null || message.trim().isEmpty()) {
            return;
        }

        // 如果是新会话，用第一条消息初始化名称
        session.initNameWithFirstMessage(message);

        // 更新最后一条消息和时间
        session.setLastMessage(message);
        session.setUpdateTime(LocalDateTime.now());

        sessionRepository.save(session);
    }

    /**
     * 检查用户是否有会话
     */
    public boolean hasSessions(User user) {
        return sessionRepository.countByUser(user) > 0;
    }

    /**
     * 获取用户的最新会话
     */
    public ChatSession getLatestSession(User user) {
        List<ChatSession> sessions = getUserSessions(user);
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    /**
     * 根据ID获取会话
     */
    public ChatSession getSessionById(Long id) {
        return sessionRepository.findById(id).orElse(null);
    }
}
