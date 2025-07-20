package com.xiaomi.controller;

import com.xiaomi.entity.User;
import com.xiaomi.repository.UserRepository;
import com.xiaomi.service.ChatService;
import com.xiaomi.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    // 修复：添加正确的依赖注入注解
    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/chat")
    public String chatPage(Model model, Authentication authentication) {
        String username = authentication.getName();

        // 修复：正确使用Optional的orElseThrow方法，提供Supplier参数
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        // 检查用户是否有会话
        model.addAttribute("hasSessions", sessionService.hasSessions(user));

        // 如果有会话，获取最新会话ID
        if (sessionService.hasSessions(user)) {
            model.addAttribute("latestSessionId", sessionService.getLatestSession(user).getId());
        }

        return "chat";
    }

    @GetMapping("/chat/stream")
    public SseEmitter streamChat(
            @RequestParam("question") String question,
            @RequestParam("sessionId") String sessionId,
            Authentication authentication) {

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));

        return chatService.getStreamingResponse(question, user, sessionId);
    }

    @GetMapping("/chat/history")
    @ResponseBody
    public List<?> getChatHistory(@RequestParam("sessionId") Long sessionId) {
        // 实现实际实现应返回该会话的历史消息
        return chatService.getSessionHistory(sessionId);
    }
}
