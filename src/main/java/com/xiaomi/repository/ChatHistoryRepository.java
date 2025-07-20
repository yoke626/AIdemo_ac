package com.xiaomi.repository;

import com.xiaomi.entity.ChatHistory;
import com.xiaomi.entity.ChatSession;
import com.xiaomi.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // 会话相关查询
    List<ChatHistory> findBySessionOrderByTimestampDesc(ChatSession session);
    //List<ChatHistory> findBySessionOrderByTimestampDesc(ChatSession session, Pageable pageable);

    // 添加按会话查询并按时间戳升序排列的方法
    List<ChatHistory> findBySessionOrderByTimestampAsc(ChatSession session);

    // 添加按会话查询并按时间戳降序排列的方法（已有的方法，保持不变）
    List<ChatHistory> findBySessionOrderByTimestampDesc(ChatSession session, org.springframework.data.domain.Pageable pageable);

    // 全局历史查询 - 按用户和时间排序
    Page<ChatHistory> findByUserOrderByTimestampDesc(User user, Pageable pageable);

    // 按用户ID查询
    Page<ChatHistory> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    // 管理员查询所有（按时间排序）
    Page<ChatHistory> findAllByOrderByTimestampDesc(Pageable pageable);
}
