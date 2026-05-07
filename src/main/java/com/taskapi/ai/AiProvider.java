package com.taskapi.ai;

public interface AiProvider {

    String chat(String systemPrompt, String userMessage);

    boolean isEnabled();
}