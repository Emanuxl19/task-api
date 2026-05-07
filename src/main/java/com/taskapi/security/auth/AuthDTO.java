package com.taskapi.security.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDTO {

    public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must contain at least one uppercase, one lowercase, one digit, and one special character"
        )
        String password
    ) {}

    public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
    ) {}

    public record OAuth2ExchangeRequest(
        @NotBlank(message = "Code is required")
        String code
    ) {}

    public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
    ) {
        public static TokenResponse of(String accessToken, String refreshToken, long expiresInMs) {
            return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInMs / 1000);
        }
    }
}
