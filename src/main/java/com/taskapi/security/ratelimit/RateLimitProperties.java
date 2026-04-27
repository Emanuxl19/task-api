package com.taskapi.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    int loginAttemptsPerMinute,
    int aiRequestsPerMinute,
    int generalRequestsPerMinute
) {
    public RateLimitProperties {
        if (loginAttemptsPerMinute <= 0) {
            loginAttemptsPerMinute = 5;
        }
        if (aiRequestsPerMinute <= 0) {
            aiRequestsPerMinute = 10;
        }
        if (generalRequestsPerMinute <= 0) {
            generalRequestsPerMinute = 60;
        }
    }
}
