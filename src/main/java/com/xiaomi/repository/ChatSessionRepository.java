package com.xiaomi.repository;

import com.xiaomi.entity.ChatSession;
import com.xiaomi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    // 查询用户的所有会话，按更新时间倒序
    List<ChatSession> findByUserOrderByUpdateTimeDesc(User user);

    // 检查用户是否有活跃会话
    long countByUser(User user);
}
