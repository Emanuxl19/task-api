package com.taskapi.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long accessTokenExpiration,
    long refreshTokenExpiration
) {
    public JwtProperties {
        if (accessTokenExpiration <= 0) accessTokenExpiration = 900_000;       // 15 min
        if (refreshTokenExpiration <= 0) refreshTokenExpiration = 604_800_000; // 7 days
    }
}
