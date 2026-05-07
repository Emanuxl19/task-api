package com.taskapi.security.jwt;

import com.taskapi.entity.Role;
import com.taskapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider")
class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    // Chave Base64 de 256 bits para testes
    private static final String TEST_SECRET =
        Base64.getEncoder().encodeToString("test-secret-key-that-is-long-enough-for-hs256!!".getBytes());

    @BeforeEach
    void setUp() {
        var properties = new JwtProperties(TEST_SECRET, 900_000, 604_800_000);
        tokenProvider = new JwtTokenProvider(properties);
    }

    private User makeUser(Long id, String name, String email, Role role) {
        try {
            var user = new User(name, email, "hashed-password");
            user.setRole(role);
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── generateAccessToken ────────────────────────────────────────────────

    @Nested
    @DisplayName("generateAccessToken()")
    class GenerateAccessToken {

        @Test
        @DisplayName("gera token JWT nao vazio")
        void shouldGenerateNonEmptyToken() {
            var user = makeUser(1L, "Ana", "ana@email.com", Role.USER);

            String token = tokenProvider.generateAccessToken(user);

            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
        }

        @Test
        @DisplayName("token contem o user id como subject")
        void shouldContainUserIdAsSubject() {
            var user = makeUser(42L, "Bruno", "bruno@email.com", Role.USER);

            String token = tokenProvider.generateAccessToken(user);
            Long extractedId = tokenProvider.getUserIdFromToken(token);

            assertThat(extractedId).isEqualTo(42L);
        }

        @Test
        @DisplayName("token contem email e role nas claims")
        void shouldContainEmailAndRoleInClaims() {
            var user = makeUser(1L, "Ana", "ana@email.com", Role.ADMIN);

            String token = tokenProvider.generateAccessToken(user);

            assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("ana@email.com");
            assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ADMIN");
        }
    }

    // ─── generateRefreshToken ───────────────────────────────────────────────

    @Nested
    @DisplayName("generateRefreshToken()")
    class GenerateRefreshToken {

        @Test
        @DisplayName("gera UUID unico a cada chamada")
        void shouldGenerateUniqueTokens() {
            String token1 = tokenProvider.generateRefreshToken();
            String token2 = tokenProvider.generateRefreshToken();

            assertThat(token1).isNotBlank();
            assertThat(token2).isNotBlank();
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ─── validateToken ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("retorna true para token valido")
        void shouldReturnTrueForValidToken() {
            var user = makeUser(1L, "Ana", "ana@email.com", Role.USER);
            String token = tokenProvider.generateAccessToken(user);

            assertThat(tokenProvider.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("retorna false para token adulterado")
        void shouldReturnFalseForTamperedToken() {
            var user = makeUser(1L, "Ana", "ana@email.com", Role.USER);
            String token = tokenProvider.generateAccessToken(user);
            String tampered = token + "x";

            assertThat(tokenProvider.validateToken(tampered)).isFalse();
        }

        @Test
        @DisplayName("retorna false para token nulo ou vazio")
        void shouldReturnFalseForNullOrEmpty() {
            assertThat(tokenProvider.validateToken(null)).isFalse();
            assertThat(tokenProvider.validateToken("")).isFalse();
            assertThat(tokenProvider.validateToken("   ")).isFalse();
        }

        @Test
        @DisplayName("retorna false para token expirado")
        void shouldReturnFalseForExpiredToken() {
            // Cria provider com expiracao de 1ms
            var shortLivedProps = new JwtProperties(TEST_SECRET, 1, 604_800_000);
            var shortLivedProvider = new JwtTokenProvider(shortLivedProps);

            var user = makeUser(1L, "Ana", "ana@email.com", Role.USER);
            String token = shortLivedProvider.generateAccessToken(user);

            // Espera o token expirar
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}

            assertThat(shortLivedProvider.validateToken(token)).isFalse();
        }

        @Test
        @DisplayName("retorna false para token assinado com chave diferente")
        void shouldReturnFalseForDifferentKey() {
            String otherSecret = Base64.getEncoder()
                .encodeToString("another-secret-key-that-is-long-enough-for-hs256!!".getBytes());
            var otherProvider = new JwtTokenProvider(new JwtProperties(otherSecret, 900_000, 604_800_000));

            var user = makeUser(1L, "Ana", "ana@email.com", Role.USER);
            String token = otherProvider.generateAccessToken(user);

            assertThat(tokenProvider.validateToken(token)).isFalse();
        }
    }

    // ─── getUserIdFromToken ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserIdFromToken()")
    class GetUserIdFromToken {

        @Test
        @DisplayName("extrai user id corretamente")
        void shouldExtractUserId() {
            var user = makeUser(99L, "Carlos", "carlos@email.com", Role.USER);
            String token = tokenProvider.generateAccessToken(user);

            assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo(99L);
        }
    }
}
