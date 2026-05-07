package com.taskapi.ai;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiProviderFactory {

    @Bean
    public AiProvider aiProvider(AiProperties.AiConfig config) {
        if (!config.isEnabled()) return new DisabledProvider();

        return switch (config.provider()) {
            case "anthropic" -> new AnthropicProvider(config);
            default          -> new OpenAiCompatibleProvider(config);
        };
    }
}