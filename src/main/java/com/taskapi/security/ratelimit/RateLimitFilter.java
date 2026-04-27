package com.taskapi.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.exception.GlobalExceptionHandler;
import com.taskapi.exception.RateLimitExceededException;
import com.taskapi.security.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final JwtTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final RateLimitStore store;

    public RateLimitFilter(RateLimitProperties properties,
                           JwtTokenProvider tokenProvider,
                           ObjectMapper objectMapper,
                           RateLimitStore store) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.store = store;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            enforceRateLimit(request);
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException ex) {
            writeTooManyRequests(response, ex.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return HttpMethod.OPTIONS.matches(request.getMethod())
            || path.startsWith("/swagger-ui")
            || path.startsWith("/api-docs")
            || path.startsWith("/v3/api-docs")
            || path.startsWith("/h2-console")
            || path.startsWith("/actuator/health");
    }

    private void enforceRateLimit(HttpServletRequest request) {
        var policy = resolvePolicy(request);
        String key = buildKey(policy, request);

        if (!store.tryAcquire(key, policy.limit(properties))) {
            throw new RateLimitExceededException(policy.message());
        }
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request) {
        String path = request.getServletPath();

        if (HttpMethod.POST.matches(request.getMethod()) && "/api/v1/auth/login".equals(path)) {
            return RateLimitPolicy.LOGIN;
        }
        if (path.startsWith("/api/v1/ai")) {
            return RateLimitPolicy.AI;
        }
        return RateLimitPolicy.GENERAL;
    }

    private String buildKey(RateLimitPolicy policy, HttpServletRequest request) {
        return switch (policy) {
            case LOGIN -> "login:ip:" + extractClientIp(request);
            case AI -> "ai:" + extractAiIdentifier(request);
            case GENERAL -> "general:ip:" + extractClientIp(request);
        };
    }

    private String extractAiIdentifier(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && tokenProvider.validateToken(token)) {
            return "user:" + tokenProvider.getUserIdFromToken(token);
        }
        return "ip:" + extractClientIp(request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void writeTooManyRequests(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        int status = HttpStatus.TOO_MANY_REQUESTS.value();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
            response.getWriter(),
            new GlobalExceptionHandler.ErrorResponse(message, status)
        );
    }

    private enum RateLimitPolicy {
        LOGIN("Too many login attempts. Please try again in a minute.") {
            @Override
            int limit(RateLimitProperties properties) {
                return properties.loginAttemptsPerMinute();
            }
        },
        AI("Too many AI requests. Please try again in a minute.") {
            @Override
            int limit(RateLimitProperties properties) {
                return properties.aiRequestsPerMinute();
            }
        },
        GENERAL("Too many requests. Please try again in a minute.") {
            @Override
            int limit(RateLimitProperties properties) {
                return properties.generalRequestsPerMinute();
            }
        };

        private final String message;

        RateLimitPolicy(String message) {
            this.message = message;
        }

        abstract int limit(RateLimitProperties properties);

        String message() {
            return message;
        }
    }
}
