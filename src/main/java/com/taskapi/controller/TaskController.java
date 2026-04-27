package com.taskapi.controller;

import com.taskapi.dto.TaskDTO.*;
import com.taskapi.entity.Task;
import com.taskapi.security.auth.CustomUserDetails;
import com.taskapi.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tasks", description = "Task management endpoints")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    // GET /api/v1/users/{userId}/tasks?status=TODO&page=0&size=10&sort=dueDate,asc
    @GetMapping("/users/{userId}/tasks")
    @Operation(summary = "List tasks by user with optional filters and pagination")
    public ResponseEntity<Page<TaskResponse>> findByUser(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @RequestParam(required = false) Task.Status status,
            @RequestParam(required = false) Task.Priority priority,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.findByUser(currentUser, userId, status, priority, pageable));
    }

    // GET /api/v1/users/{userId}/tasks/overdue
    @GetMapping("/users/{userId}/tasks/overdue")
    @Operation(summary = "List overdue tasks for a user")
    public ResponseEntity<List<TaskResponse>> findOverdue(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId) {
        return ResponseEntity.ok(service.findOverdue(currentUser, userId));
    }

    @GetMapping("/tasks/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskResponse> findById(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.findById(currentUser, id));
    }

    // POST /api/v1/users/{userId}/tasks
    @PostMapping("/users/{userId}/tasks")
    @Operation(summary = "Create task for a user")
    public ResponseEntity<TaskResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long userId,
            @Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(currentUser, userId, request));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Update task")
    public ResponseEntity<TaskResponse> update(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request) {
        return ResponseEntity.ok(service.update(currentUser, id, request));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Delete task")
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {
        service.delete(currentUser, id);
        return ResponseEntity.noContent().build();
    }
}
