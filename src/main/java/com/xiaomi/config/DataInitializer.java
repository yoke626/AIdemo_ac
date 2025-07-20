package com.xiaomi.config;

import com.xiaomi.entity.User;
import com.xiaomi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 引入这个类
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    // 【关键改动1】: 删除下面这行 @Autowired 注入
    // @Autowired
    // private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // 【关键改动2】: 在方法内部直接创建一个新的加密器实例
        // 这就打破了 DataInitializer 对 SecurityConfig 的依赖循环
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // 如果不存在名为 "admin" 的用户，则创建一个
        if (!userRepository.findByUsername("admin").isPresent()) {
            User admin = new User();
            admin.setUsername("admin");
            // 使用我们在这里创建的加密器实例
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setEmail("admin@example.com");
            admin.setRole("ROLE_ADMIN");
            userRepository.save(admin);
            System.out.println("创建了默认管理员账户 'admin'，密码 'admin123'");
        }
    }
}
