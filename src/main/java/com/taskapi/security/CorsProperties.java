package com.taskapi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
    List<String> allowedOrigins,
    List<String> allowedMethods,
    List<String> allowedHeaders,
    boolean allowCredentials
) {
    public CorsProperties {
        if (allowedOrigins == null) allowedOrigins = List.of("http://localhost:3000");
        if (allowedMethods == null) allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
        if (allowedHeaders == null) allowedHeaders = List.of("*");
    }
}
