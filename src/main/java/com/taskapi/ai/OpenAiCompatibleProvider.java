package com.taskapi.ai;

import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OpenAiCompatibleProvider implements AiProvider {

    private final AiProperties.AiConfig config;
    private final RestClient restClient;

    // baseUrls padrão por provider
    private static final Map<String, String> BASE_URLS = Map.of(
            "openai",   "https://api.openai.com/v1",
            "kimi",     "https://api.moonshot.cn/v1",
            "qwen",     "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
            "deepseek", "https://api.deepseek.com/v1",
            "groq",     "https://api.groq.com/openai/v1",
            "ollama",   "http://localhost:11434/v1"
    );

    public OpenAiCompatibleProvider(AiProperties.AiConfig config) {
        this.config = config;
        String baseUrl = config.baseUrl() != null
                ? config.baseUrl()
                : BASE_URLS.getOrDefault(config.provider(), "https://api.openai.com/v1");

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        var body = Map.of(
                "model", config.model(),
                "messages", List.of(
                        Map.of("role", "system",  "content", systemPrompt),
                        Map.of("role", "user",    "content", userMessage)
                )
        );

        var response = restClient.post()
                .uri("/chat/completions")
                .body(body)
                .retrieve()
                .body(OpenAiResponse.class);

        return response.choices().get(0).message().content();
    }

    @Override
    public boolean isEnabled() { return config.isEnabled(); }

    public record OpenAiResponse(List<Choice> choices) {
        public record Choice(Message message) {}
        public record Message(String content) {}
    }
}