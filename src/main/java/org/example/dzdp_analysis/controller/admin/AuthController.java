package org.example.dzdp_analysis.controller.admin;

import org.example.dzdp_analysis.repository.dto.login.LoginRequest;
import org.example.dzdp_analysis.repository.dto.login.LoginResponse;
import org.example.dzdp_analysis.repository.dao.admin.UserRepository;
import org.example.dzdp_analysis.repository.dto.login.RegisterRequest;
import org.example.dzdp_analysis.repository.dto.login.RegisterResponse;
import org.example.dzdp_analysis.repository.entity.admin.User;
import org.example.dzdp_analysis.util.JwtUtil;
import org.example.dzdp_analysis.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordUtil passwordUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        logger.info("登录请求: username={}", loginRequest.getUsername());

        try {
            // 确保用户名使用UTF-8编码处理
            String username = new String(loginRequest.getUsername().getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                logger.warn("用户不存在: {}", username);
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户名或密码错误"
                ));
            }

            User user = userOptional.get();
            logger.info("找到用户: {}, 密码哈希: {}", user.getUsername(), user.getPassword());

            // 检查账户状态
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                logger.warn("账户状态异常: {}", user.getStatus());
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "账户已被禁用或锁定"
                ));
            }

            // 验证密码
            String rawPassword = loginRequest.getPassword();
            String encodedPassword = user.getPassword();

            logger.info("原始密码: {}", rawPassword);
            logger.info("数据库密码: {}", encodedPassword);
            logger.info("密码哈希长度: {}", encodedPassword.length());
            logger.info("密码哈希前缀: {}", encodedPassword.substring(0, Math.min(10, encodedPassword.length())));

            boolean passwordMatches = passwordEncoder.matches(rawPassword, encodedPassword);
            logger.info("密码匹配结果: {}", passwordMatches);

            if (!passwordMatches) {
                // 尝试使用原始用户名再次查找（处理编码问题）
                Optional<User> altUserOptional = userRepository.findByUsername(loginRequest.getUsername());
                if (altUserOptional.isPresent() && !altUserOptional.get().getUserId().equals(user.getUserId())) {
                    User altUser = altUserOptional.get();
                    boolean altMatch = passwordEncoder.matches(rawPassword, altUser.getPassword());
                    logger.info("备用用户验证结果: {}", altMatch);
                    if (altMatch) {
                        user = altUser; // 使用备用用户
                        passwordMatches = true;
                    }
                }
            }

            if (!passwordMatches) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户名或密码错误"
                ));
            }

            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);

            // 生成JWT令牌
            String token = jwtUtil.generateToken(user.getUsername(), user.getUserId(), user.getRole().name());

            LoginResponse response = new LoginResponse();
            response.setSuccess(true);
            response.setMessage("登录成功");
            response.setToken(token);
            response.setUser(new LoginResponse.UserInfo(user));
            logger.info("登录成功: {}", user.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("登录处理异常: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "登录处理失败"
            ));
        }
    }
    // 添加调试接口
    @GetMapping("/debug/password")
    public ResponseEntity<?> debugPassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return ResponseEntity.ok(Map.of(
                "raw", password,
                "encoded", encoded,
                "matches", passwordEncoder.matches(password, encoded)
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        // 验证角色
        if (!"MERCHANT".equals(registerRequest.getRole()) && !"CUSTOMER".equals(registerRequest.getRole())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "注册角色必须是MERCHANT或CUSTOMER"
            ));
        }


        // 检查用户名是否已存在
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "用户名已存在"
            ));
        }

        // 检查邮箱是否已存在
        if (registerRequest.getEmail() != null && userRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "邮箱已存在"
            ));
        }

        try {
            User.UserRole role = User.UserRole.valueOf(registerRequest.getRole());

            User user = new User();
            user.setUsername(registerRequest.getUsername());
            user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
            user.setRole(role);
            user.setEmail(registerRequest.getEmail());
            user.setPhone(registerRequest.getPhone());
            user.setStatus(User.UserStatus.ACTIVE);

            // 如果是商户，设置商户名称
            if (role == User.UserRole.MERCHANT) {
                if (registerRequest.getMerchantName() == null || registerRequest.getMerchantName().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "商户名称不能为空"
                    ));
                }
                user.setMerchantName(registerRequest.getMerchantName());
            }

            User savedUser = userRepository.save(user);

            RegisterResponse response = new RegisterResponse();
            response.setSuccess(true);
            response.setMessage("注册成功");
            response.setUser(new RegisterResponse.UserInfo(savedUser));

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "无效的角色类型"
            ));
        }
    }
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("authenticated", true);
        response.put("username", jwtUtil.extractUsername(token));
        response.put("userId", jwtUtil.extractUserId(token));
        response.put("role", jwtUtil.extractRole(token));

        return ResponseEntity.ok(response);
    }
    @GetMapping("/check-username/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        boolean exists = userRepository.existsByUsername(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<?> checkEmail(@PathVariable String email) {
        boolean exists = userRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }
    // 在AuthController中添加
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String username,
                                           @RequestParam String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "用户不存在"));
        }

        User user = userOptional.get();
        String oldHash = user.getPassword();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "密码重置成功",
                "old_hash", oldHash,
                "new_hash", user.getPassword()
        ));
    }

    // 调试接口，显示密码编码信息
    @GetMapping("/password-info")
    public ResponseEntity<?> getPasswordInfo(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return ResponseEntity.ok(Map.of(
                "raw_password", password,
                "encoded_password", encoded,
                "length", encoded.length(),
                "prefix", encoded.substring(0, 7),
                "matches_self", passwordEncoder.matches(password, encoded)
        ));
    }


}