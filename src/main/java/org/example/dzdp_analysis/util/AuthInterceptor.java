package org.example.dzdp_analysis.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dzdp_analysis.util.JwtUtil;
import org.example.dzdp_analysis.repository.dao.admin.UserRepository;
import org.example.dzdp_analysis.repository.entity.admin.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserRepository userRepository;
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/check-username",
            "/api/auth/check-email",
            "/api/health",
            "/api/test",
            "/api/cluster/health"
    );
    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 允许OPTIONS请求直接通过
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        // 放行列表
        if (path.startsWith("/api/auth") ||
                path.startsWith("/api/health") ||
                path.startsWith("/api/test") ||
                path.startsWith("/api/cluster") ||
                path.startsWith("/api/status") ||
                path.equals("/api/etl/stats") ||
                path.equals("/api/etl/tasks")) {
            return true;
        }


        // 设置响应类型为JSON
        response.setContentType("application/json;charset=UTF-8");
        
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"未提供认证令牌\"}");
            return false;
        }

        token = token.substring(7);
        try {
            if (!jwtUtil.validateToken(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"认证令牌无效或已过期\"}");
                return false;
            }

            // 获取用户信息并验证用户状态
            String username = jwtUtil.extractUsername(token);
            Optional<User> userOptional = userRepository.findByUsername(username);
            
            if (userOptional.isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"用户不存在\"}");
                return false;
            }
            
            User user = userOptional.get();
            if (user.getStatus() != User.UserStatus.ACTIVE) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"账户已被禁用\"}");
                return false;
            }

            // 检查管理员权限
            String role = user.getRole().name(); // 从数据库中获取最新角色信息，而不是token
            if (request.getRequestURI().startsWith("/api/admin/") && !"ADMIN".equals(role)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("{\"error\": \"权限不足，需要管理员角色\"}");
                return false;
            }

            request.setAttribute("userId", user.getUserId());
            request.setAttribute("username", username);
            request.setAttribute("role", role);

            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"认证失败：" + e.getMessage() + "\"}");
            return false;
        }

    }
}