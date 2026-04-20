package com.taskapi.dto;

import com.taskapi.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDTO {

    // ─── REQUEST ─────────────────────────────────────────────────────────────

    public record CreateUserRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email

    ) {}

    public record UpdateUserRequest(

        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Email(message = "Invalid email format")
        String email

    ) {}

    // ─── RESPONSE ────────────────────────────────────────────────────────────

    public record UserResponse(
        Long id,
        String name,
        String email,
        int taskCount
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getTasks().size()
            );
        }
    }
}
