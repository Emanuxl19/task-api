package com.taskapi.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiDTO {

    public record AiRequest(
        @NotBlank(message = "Domain is required")
        String domain,

        @NotBlank(message = "Question is required")
        @Size(max = 2000, message = "Question cannot exceed 2000 characters")
        String question
    ) {}

    public record AiResponse(
        String answer,
        String provider
    ) {}

    public record AiStatusResponse(
        boolean enabled,
        String provider
    ) {}
}