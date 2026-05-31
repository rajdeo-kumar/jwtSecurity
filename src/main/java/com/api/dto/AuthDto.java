package com.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;

        @NotBlank
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 30)
        private String username;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 6, max = 60)
        private String password;
    }

    @Data
    public static class JwtResponse {
        private String token;
        private String type = "Bearer";
        private String username;
        private List<String> roles;
        private LocalDateTime expiresAt;

        public JwtResponse(String token, String username, List<String> roles, LocalDateTime expiresAt) {
            this.token = token;
            this.username = username;
            this.roles = roles;
            this.expiresAt = expiresAt;
        }
    }
}
