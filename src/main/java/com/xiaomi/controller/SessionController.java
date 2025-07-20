package com.xiaomi.controller;

import com.xiaomi.entity.ChatSession;
import com.xiaomi.entity.User;
import com.xiaomi.repository.UserRepository;
import com.xiaomi.service.ChatService;
import com.xiaomi.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/sessions")
public class SessionController {

    // 日期时间格式化器
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private SessionService sessionService;

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 获取当前用户的所有会话
     */
    @GetMapping
    public List<?> getUserSessions(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 返回简化的会话信息给前端
        return sessionService.getUserSessions(user).stream()
                .map(session -> {
                    // 修复：将LocalDateTime转换为String
                    String formattedTime = session.getUpdateTime() != null
                            ? session.getUpdateTime().format(DATE_FORMATTER)
                            : "";

                    return new SessionDTO(
                            session.getId(),
                            session.getSessionName(),
                            session.getLastMessage(),
                            formattedTime  // 使用格式化后的时间字符串
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建新会话
     */
    @PostMapping("/new")
    public ChatSession createNewSession(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        return sessionService.createNewSession(user);
    }

    /**
     * 获取会话的消息历史
     */
    @GetMapping("/{sessionId}/messages")
    public List<?> getSessionMessages(@PathVariable Long sessionId) {
        return chatService.getSessionHistory(sessionId);
    }

    // 内部DTO类，用于传输会话信息
    private static class SessionDTO {
        private Long id;
        private String sessionName;
        private String lastMessage;
        private String updateTime;  // 保持为String类型

        // 构造函数参数保持为String类型
        public SessionDTO(Long id, String sessionName, String lastMessage, String updateTime) {
            this.id = id;
            this.sessionName = sessionName;
            this.lastMessage = lastMessage;
            this.updateTime = updateTime;
        }

        // Getters
        public Long getId() { return id; }
        public String getSessionName() { return sessionName; }
        public String getLastMessage() { return lastMessage; }
        public String getUpdateTime() { return updateTime; }
    }
}
