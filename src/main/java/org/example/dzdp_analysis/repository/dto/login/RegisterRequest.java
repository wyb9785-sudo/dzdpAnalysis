package org.example.dzdp_analysis.repository.dto.login;


import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String email;
    private String phone;
    private String role; // MERCHANT 或 CUSTOMER
    private String merchantName; // 仅当role为MERCHANT时有效
}