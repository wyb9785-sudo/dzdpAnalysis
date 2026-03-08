package org.example.dzdp_analysis.repository.dto.login;


import lombok.Data;

@Data
public class RegisterResponse {
    private boolean success;
    private String message;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long userId;
        private String username;
        private String role;
        private String email;

        public UserInfo(org.example.dzdp_analysis.repository.entity.admin.User user) {
            this.userId = user.getUserId();
            this.username = user.getUsername();
            this.role = user.getRole().name();
            this.email = user.getEmail();
        }
    }
}
