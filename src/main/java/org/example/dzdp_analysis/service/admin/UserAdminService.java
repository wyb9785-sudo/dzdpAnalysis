package org.example.dzdp_analysis.service.admin;

import org.example.dzdp_analysis.repository.dao.admin.UserRepository;
import org.example.dzdp_analysis.repository.entity.admin.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserAdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<User> getUsersWithFilters(String search, String role, String status, Pageable pageable) {
        try {
            // 添加参数验证
            if (search != null && !search.trim().isEmpty()) {
                return userRepository.findByUsernameContainingOrEmailContaining(search, pageable);
            } else if (role != null && status != null) {
                // 确保角色和状态值有效
                User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
                User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
                return userRepository.findByRoleAndStatus(userRole, userStatus, pageable);
            } else if (role != null) {
                User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
                return userRepository.findByRole(userRole, pageable);
            } else if (status != null) {
                User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
                return userRepository.findByStatus(userStatus, pageable);
            } else {
                // 使用默认排序
                return userRepository.findAllByOrderByCreateTimeDesc(pageable);
            }
        } catch (IllegalArgumentException e) {
            // 处理无效的枚举值
            throw new IllegalArgumentException("无效的角色或状态值: " + e.getMessage());
        } catch (Exception e) {
            // 处理其他异常
            throw new RuntimeException("查询用户列表失败: " + e.getMessage());
        }
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 检查邮箱是否已存在
        if (user.getEmail() != null && userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在");
        }

        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User userDetails) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOptional.get();

        // 检查邮箱是否被其他用户使用
        if (userDetails.getEmail() != null &&
                !userDetails.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(userDetails.getEmail())) {
            throw new IllegalArgumentException("邮箱已被其他用户使用");
        }

        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        user.setRole(userDetails.getRole());
        user.setStatus(userDetails.getStatus());
        user.setMerchantName(userDetails.getMerchantName());
        user.setUpdateTime(LocalDateTime.now());

        // 如果提供了新密码，则加密更新
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("用户不存在");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public User updateUserStatus(Long id, String status) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOptional.get();
        try {
            User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
            user.setStatus(userStatus);
            user.setUpdateTime(LocalDateTime.now());
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的状态值");
        }
    }
}