package com.taskapi.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class AiProperties {

    @Bean
    public AiConfig aiConfig() throws IOException {
        Path path = Path.of(System.getProperty("user.home"),
                                ".config", "task-api", "ai-config.json");
        if (!Files.exists(path)) {
            return AiConfig.disabled();
        }
        return new ObjectMapper().readValue(path.toFile(), AiConfig.class);
    }

    public record AiConfig(
        String provider,
        String apiKey,
        String model,
        String baseUrl
    ) {
        public static AiConfig disabled() {
            return new AiConfig(null, null, null, null);
        }

        public boolean isEnabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

}

