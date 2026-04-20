package com.taskapi.dto;

import com.taskapi.entity.Task;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTOs usando Records — Java 16+
 *
 * Records são imutáveis e eliminam a necessidade de Lombok.
 * O compilador gera automaticamente: construtor, getters, equals, hashCode e toString.
 *
 * IMPORTANTE: DTOs nunca expõem a entidade diretamente.
 * Isso protege dados internos e desacopla a API do banco.
 */
public class TaskDTO {


    public record CreateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Priority is required")
        Task.Priority priority,

        @Future(message = "Due date must be in the future")
        LocalDate dueDate

    ) {}

    public record UpdateTaskRequest(

        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        Task.Status status,

        Task.Priority priority,

        LocalDate dueDate

    ) {}


    public record TaskResponse(
        Long id,
        String title,
        String description,
        Task.Status status,
        Task.Priority priority,
        LocalDate dueDate,
        LocalDateTime createdAt,
        Long userId,
        String userName
    ) {
        // Factory method — converte entidade em DTO
        // Evita lógica de mapeamento espalhada pelo código
        public static TaskResponse from(Task task) {
            return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUser().getId(),
                task.getUser().getName()
            );
        }
    }
}
