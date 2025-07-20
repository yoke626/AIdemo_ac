package com.xiaomi.service;

import com.xiaomi.entity.ChatHistory;
import com.xiaomi.entity.ChatSession;
import com.xiaomi.entity.User;
import com.xiaomi.repository.ChatHistoryRepository;
import com.xiaomi.repository.ChatSessionRepository;
import com.xiaomi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HistoryService {

    @Autowired
    private ChatHistoryRepository historyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatSessionRepository sessionRepository;


    // 日期格式化器
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 返回包含格式化日期的Map列表，避免模板层处理日期
     */
    public Page<Map<String, Object>> getHistoryWithFormattedDates(Authentication authentication, Pageable pageable) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role));

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        Page<ChatHistory> historyPage;
        if (isAdmin) {
            historyPage = historyRepository.findAllByOrderByTimestampDesc(pageable);
        } else {
            historyPage = historyRepository.findByUserOrderByTimestampDesc(currentUser, pageable);
        }

        // 转换为Map并预先格式化日期
        List<Map<String, Object>> formattedHistory = historyPage.getContent().stream()
                .filter(history -> {
                    ChatSession session = history.getSession();
                    return session != null && sessionRepository.existsById(session.getId()) && history.getTimestamp() != null;
                })
                .map(history -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", history.getId());
                    map.put("question", history.getQuestion());
                    map.put("answer", history.getAnswer());
                    map.put("sessionId", history.getSession().getId());
                    map.put("username", history.getUser().getUsername());

                    // 在服务层格式化日期
                    LocalDateTime timestamp = history.getTimestamp();
                    map.put("formattedTimestamp", timestamp != null ? formatter.format(timestamp) : "无时间记录");

                    return map;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(formattedHistory, pageable, historyPage.getTotalElements());
    }
    public Page<ChatHistory> getHistoryByRole(Authentication authentication, Pageable pageable) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role));

        String username = authentication.getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        Page<ChatHistory> historyPage;
        if (isAdmin) {
            historyPage = historyRepository.findAllByOrderByTimestampDesc(pageable);
        } else {
            historyPage = historyRepository.findByUserOrderByTimestampDesc(currentUser, pageable);
        }

        // 过滤掉会话不存在或时间戳为null的记录（关键修复）
        List<ChatHistory> filteredContent = historyPage.getContent().stream()
                .filter(history -> {
                    // 检查会话是否存在
                    ChatSession session = history.getSession();
                    if (session == null || !sessionRepository.existsById(session.getId())) {
                        return false;
                    }
                    // 检查时间戳是否为null
                    return history.getTimestamp() != null;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(filteredContent, pageable, historyPage.getTotalElements());
    }

    public Page<ChatHistory> getUserHistory(Long userId, Authentication authentication, Pageable pageable) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_ADMIN".equals(role));

        if (!isAdmin) {
            throw new SecurityException("权限不足：只有管理员可以查看其他用户的历史记录");
        }

        Page<ChatHistory> historyPage = historyRepository.findByUserIdOrderByTimestampDesc(userId, pageable);

        // 同样过滤无效记录
        List<ChatHistory> filteredContent = historyPage.getContent().stream()
                .filter(history -> history.getTimestamp() != null)
                .collect(Collectors.toList());

        return new PageImpl<>(filteredContent, pageable, historyPage.getTotalElements());
    }
}
