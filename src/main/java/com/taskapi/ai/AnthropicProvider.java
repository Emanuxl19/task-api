package com.taskapi.ai;

import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class AnthropicProvider implements AiProvider {

    private final AiProperties.AiConfig config;
    private final RestClient restClient;

    public AnthropicProvider(AiProperties.AiConfig config) {
        this.config = config;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key",         config.apiKey())
                .defaultHeader("anthropic-version",  "2023-06-01")
                .defaultHeader("Content-Type",       "application/json")
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        var body = Map.of(
                "model",      config.model(),
                "max_tokens", 1024,
                "system",     systemPrompt,
                "messages",   List.of(Map.of("role", "user", "content", userMessage))
        );

        var response = restClient.post()
                .uri("/v1/messages")
                .body(body)
                .retrieve()
                .body(AnthropicResponse.class);

        return response.content().get(0).text();
    }

    @Override
    public boolean isEnabled() { return config.isEnabled(); }

    public record AnthropicResponse(List<Content> content) {
        public record Content(String text) {}
    }
}