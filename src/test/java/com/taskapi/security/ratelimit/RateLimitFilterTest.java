package com.taskapi.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.entity.Role;
import com.taskapi.entity.User;
import com.taskapi.security.jwt.JwtProperties;
import com.taskapi.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    private static final String TEST_SECRET =
        Base64.getEncoder().encodeToString("test-secret-key-that-is-long-enough-for-hs256!!".getBytes());

    private MutableClock clock;
    private RateLimitFilter filter;
    private JwtTokenProvider tokenProvider;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-04-23T12:00:00Z"), ZoneOffset.UTC);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        tokenProvider = new JwtTokenProvider(new JwtProperties(TEST_SECRET, 900_000, 604_800_000));
        var store = new InMemoryRateLimitStore(clock);
        filter = new RateLimitFilter(
            new RateLimitProperties(5, 10, 60),
            tokenProvider,
            objectMapper,
            store
        );
    }

    @Nested
    @DisplayName("login requests")
    class LoginRequests {

        @Test
        @DisplayName("limits login attempts per IP")
        void shouldLimitLoginAttemptsPerIp() throws Exception {
            for (int i = 0; i < 5; i++) {
                var result = invoke("POST", "/api/v1/auth/login", "10.0.0.1", null);
                assertThat(result.status()).isEqualTo(200);
                assertThat(result.chainInvocations()).isEqualTo(1);
            }

            var denied = invoke("POST", "/api/v1/auth/login", "10.0.0.1", null);

            assertThat(denied.status()).isEqualTo(429);
            assertThat(denied.chainInvocations()).isEqualTo(0);
            assertThat(denied.json().get("message").asText())
                .isEqualTo("Too many login attempts. Please try again in a minute.");
        }
    }

    @Nested
    @DisplayName("AI requests")
    class AiRequests {

        @Test
        @DisplayName("limits AI requests per user and not only by IP")
        void shouldLimitAiRequestsPerUser() throws Exception {
            String userOneToken = tokenProvider.generateAccessToken(makeUser(1L, Role.USER));
            String userTwoToken = tokenProvider.generateAccessToken(makeUser(2L, Role.USER));

            for (int i = 0; i < 10; i++) {
                var result = invoke("POST", "/api/v1/ai/ask", "10.0.0.1", userOneToken);
                assertThat(result.status()).isEqualTo(200);
                assertThat(result.chainInvocations()).isEqualTo(1);
            }

            var denied = invoke("POST", "/api/v1/ai/ask", "10.0.0.1", userOneToken);
            var allowedForAnotherUser = invoke("POST", "/api/v1/ai/ask", "10.0.0.1", userTwoToken);

            assertThat(denied.status()).isEqualTo(429);
            assertThat(denied.chainInvocations()).isEqualTo(0);
            assertThat(denied.json().get("message").asText())
                .isEqualTo("Too many AI requests. Please try again in a minute.");

            assertThat(allowedForAnotherUser.status()).isEqualTo(200);
            assertThat(allowedForAnotherUser.chainInvocations()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("general requests")
    class GeneralRequests {

        @Test
        @DisplayName("limits general requests per IP")
        void shouldLimitGeneralRequestsPerIp() throws Exception {
            for (int i = 0; i < 60; i++) {
                var result = invoke("GET", "/api/v1/tasks/1", "10.0.0.1", null);
                assertThat(result.status()).isEqualTo(200);
                assertThat(result.chainInvocations()).isEqualTo(1);
            }

            var denied = invoke("GET", "/api/v1/tasks/1", "10.0.0.1", null);

            assertThat(denied.status()).isEqualTo(429);
            assertThat(denied.chainInvocations()).isEqualTo(0);
            assertThat(denied.json().get("message").asText())
                .isEqualTo("Too many requests. Please try again in a minute.");
        }

        @Test
        @DisplayName("allows requests again after the sliding window expires")
        void shouldAllowRequestsAgainAfterWindowExpires() throws Exception {
            for (int i = 0; i < 60; i++) {
                invoke("GET", "/api/v1/tasks/1", "10.0.0.1", null);
            }

            var denied = invoke("GET", "/api/v1/tasks/1", "10.0.0.1", null);
            clock.advanceSeconds(61);
            var allowed = invoke("GET", "/api/v1/tasks/1", "10.0.0.1", null);

            assertThat(denied.status()).isEqualTo(429);
            assertThat(allowed.status()).isEqualTo(200);
            assertThat(allowed.chainInvocations()).isEqualTo(1);
        }
    }

    private InvocationResult invoke(String method, String path, String remoteAddr, String token)
            throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setRemoteAddr(remoteAddr);

        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }

        var response = new MockHttpServletResponse();
        var chainInvocations = new AtomicInteger();

        filter.doFilter(request, response, (req, res) -> chainInvocations.incrementAndGet());

        return new InvocationResult(response, chainInvocations.get(), objectMapper);
    }

    private User makeUser(Long id, Role role) {
        var user = new User("User " + id, "user" + id + "@email.com", "secret");
        user.setRole(role);
        setField(User.class, user, "id", id);
        return user;
    }

    private void setField(Class<?> type, Object target, String fieldName, Object value) {
        try {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record InvocationResult(
        MockHttpServletResponse response,
        int chainInvocations,
        ObjectMapper objectMapper
    ) {
        int status() {
            return response.getStatus();
        }

        com.fasterxml.jackson.databind.JsonNode json() throws Exception {
            return objectMapper.readTree(response.getContentAsString());
        }
    }

    private static final class MutableClock extends Clock {

        private Instant current;
        private final ZoneId zone;

        private MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }

        void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }
    }
}
