package org.example.dzdp_analysis.util;

import org.example.dzdp_analysis.repository.dao.admin.UserRepository;
import org.example.dzdp_analysis.repository.entity.admin.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // 初始化管理员用户
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole(User.UserRole.ADMIN);
            admin.setEmail("admin@dzdp.com");
            admin.setPhone("13800138000");
            admin.setStatus(User.UserStatus.ACTIVE);

            userRepository.save(admin);
            System.out.println("初始化管理员用户: admin/admin123");
            // 输出加密后的密码用于调试
            String encodedPassword = passwordEncoder.encode("admin123");
            System.out.println("加密后的密码: " + encodedPassword);
        }
    }
}