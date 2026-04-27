package com.taskapi.ai;

public class AiNotConfiguredException extends RuntimeException {

    public AiNotConfiguredException() {
        super("IA não configurada. Crie o arquivo ~/.config/task-api/ai-config.json " +
              "com provider, apiKey e model.");
    }
}