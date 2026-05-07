package com.taskapi.ai;

public class DisabledProvider implements AiProvider {

    @Override
    public String chat(String systemPrompt, String userMessage) {
        throw new AiNotConfiguredException();
    }

    @Override
    public boolean isEnabled() { return false; }
}