package com.taskapi.security.oauth2;

import com.taskapi.security.auth.AuthDTO.TokenResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Single-use, short-lived authorization codes for the OAuth2 redirect flow.
 *
 * The OAuth2 success handler issues a `code` instead of putting tokens in the
 * redirect URL; the frontend exchanges it via POST /auth/oauth2/exchange.
 *
 * In-memory only — for multi-instance deployments back this with Redis (same
 * pattern as RateLimitStore).
 */
@Component
public class OAuth2AuthorizationCodeStore {

    static final Duration TTL = Duration.ofSeconds(60);

    private final ConcurrentMap<String, Entry> codes = new ConcurrentHashMap<>();

    public String issue(TokenResponse tokens) {
        purgeExpired();
        String code = UUID.randomUUID().toString();
        codes.put(code, new Entry(tokens, Instant.now().plus(TTL)));
        return code;
    }

    public Optional<TokenResponse> consume(String code) {
        if (code == null) {
            return Optional.empty();
        }
        Entry entry = codes.remove(code);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(entry.tokens());
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        codes.entrySet().removeIf(e -> e.getValue().expiresAt().isBefore(now));
    }

    private record Entry(TokenResponse tokens, Instant expiresAt) {}
}
