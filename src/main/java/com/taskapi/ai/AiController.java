package com.taskapi.ai;

import com.taskapi.ai.AiDTO.AiRequest;
import com.taskapi.ai.AiDTO.AiResponse;
import com.taskapi.ai.AiDTO.AiStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "AI assistant endpoints")
public class AiController {

    private final AiService service;

    public AiController(AiService service) {
        this.service = service;
    }

    @PostMapping("/ask")
    @Operation(summary = "Ask a domain-specific question to the AI")
    public ResponseEntity<AiResponse> ask(@Valid @RequestBody AiRequest request)
            throws IOException {
        return ResponseEntity.ok(service.ask(request));
    }

    @GetMapping("/status")
    @Operation(summary = "Check if AI is configured and which provider is active")
    public ResponseEntity<AiStatusResponse> status() {
        return ResponseEntity.ok(service.status());
    }
}