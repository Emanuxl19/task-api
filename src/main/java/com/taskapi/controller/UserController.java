package com.taskapi.controller;

import com.taskapi.dto.UserDTO.*;
import com.taskapi.security.auth.CustomUserDetails;
import com.taskapi.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user profile")
    public ResponseEntity<UserResponse> me(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(service.findById(currentUser.getId()));
    }

    @GetMapping
    @Operation(summary = "List all users")
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (admin only — regular users should use /users/me)")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new user")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        var created = service.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();   // 204 No Content
    }
}
