package com.xiaomi.repository;

import com.xiaomi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 用于通过用户名查询用户
    Optional<User> findByUsername(String username);
}
