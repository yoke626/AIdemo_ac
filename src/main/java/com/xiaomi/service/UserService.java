package com.xiaomi.service;

import com.xiaomi.entity.User;
import com.xiaomi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void register(User user) {
        userRepository.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new IllegalStateException("用户名已存在!");
        });
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 为新注册的用户设置默认角色
        user.setRole("ROLE_USER");

        userRepository.save(user);
    }
}
