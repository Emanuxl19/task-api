package com.taskapi.security.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(String redirectUri) {
    public OAuth2Properties {
        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = "http://localhost:3000/oauth2/callback";
        }
    }
}
