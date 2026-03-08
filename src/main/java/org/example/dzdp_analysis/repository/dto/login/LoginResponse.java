package org.example.dzdp_analysis.repository.dto.login;

import lombok.Data;
import org.example.dzdp_analysis.repository.entity.admin.User;

@Data
public class LoginResponse {
    private boolean success;
    private String message;
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private User.UserRole role;
        private String email;
        private String phone;

        public UserInfo(User user) {
            this.userId = user.getUserId();
            this.username = user.getUsername();
            this.role = user.getRole();
            this.email = user.getEmail();
            this.phone = user.getPhone();
        }
    }
}