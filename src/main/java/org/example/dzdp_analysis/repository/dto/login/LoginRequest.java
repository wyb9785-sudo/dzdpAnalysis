package org.example.dzdp_analysis.repository.dto.login;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}