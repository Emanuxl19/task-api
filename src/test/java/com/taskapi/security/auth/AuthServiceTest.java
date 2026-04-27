package com.taskapi.security.auth;

import com.taskapi.entity.RefreshToken;
import com.taskapi.entity.Role;
import com.taskapi.entity.User;
import com.taskapi.exception.EmailAlreadyExistsException;
import com.taskapi.exception.InvalidTokenException;
import com.taskapi.repository.RefreshTokenRepository;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.AuthDTO.*;
import com.taskapi.security.jwt.JwtProperties;
import com.taskapi.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;
    @Mock JwtProperties jwtProperties;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks AuthService authService;

    private User makeUser(Long id, String name, String email) {
        try {
            var user = new User(name, email, "hashed");
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── register ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("cria usuario e retorna tokens quando email nao existe")
        void shouldCreateUserAndReturnTokens() {
            var request = new RegisterRequest("Ana", "ana@email.com", "Senha@123");
            var savedUser = makeUser(1L, "Ana", "ana@email.com");

            when(userRepository.existsByEmail("ana@email.com")).thenReturn(false);
            when(passwordEncoder.encode("Senha@123")).thenReturn("hashed");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(tokenProvider.generateAccessToken(savedUser)).thenReturn("access-token");
            when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(jwtProperties.accessTokenExpiration()).thenReturn(900_000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = authService.register(request);

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
            assertThat(result.tokenType()).isEqualTo("Bearer");
            assertThat(result.expiresIn()).isEqualTo(900);

            verify(userRepository).save(any(User.class));
            verify(refreshTokenRepository).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("lanca EmailAlreadyExistsException quando email ja existe")
        void shouldThrowWhenEmailExists() {
            var request = new RegisterRequest("Ana", "ana@email.com", "Senha@123");
            when(userRepository.existsByEmail("ana@email.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // ─── login ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("retorna tokens quando credenciais sao validas")
        void shouldReturnTokensOnValidCredentials() {
            var request = new LoginRequest("ana@email.com", "Senha@123");
            var user = makeUser(1L, "Ana", "ana@email.com");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("ana@email.com", null));
            when(userRepository.findByEmail("ana@email.com")).thenReturn(Optional.of(user));
            when(tokenProvider.generateAccessToken(user)).thenReturn("access-token");
            when(tokenProvider.generateRefreshToken()).thenReturn("refresh-token");
            when(jwtProperties.accessTokenExpiration()).thenReturn(900_000L);
            when(jwtProperties.refreshTokenExpiration()).thenReturn(604_800_000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = authService.login(request);

            assertThat(result.accessToken()).isEqualTo("access-token");
            assertThat(result.refreshToken()).isEqualTo("refresh-token");
        }

        @Test
        @DisplayName("propaga BadCredentialsException quando credenciais sao invalidas")
        void shouldThrowOnInvalidCredentials() {
            var request = new LoginRequest("ana@email.com", "wrong");

            when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
        }
    }

    // ─── refresh ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refresh()")
    class Refresh {

        @Test
        @DisplayName("retorna novos tokens e revoga o antigo")
        void shouldRotateTokens() {
            var request = new RefreshRequest("old-refresh-token");
            var user = makeUser(1L, "Ana", "ana@email.com");
            var oldToken = new RefreshToken("old-refresh-token", user,
                Instant.now().plusSeconds(3600));

            when(refreshTokenRepository.findByToken("old-refresh-token"))
                .thenReturn(Optional.of(oldToken));
            when(refreshTokenRepository.revokeIfActive(eq("old-refresh-token"), any(Instant.class)))
                .thenReturn(1);
            when(tokenProvider.generateAccessToken(user)).thenReturn("new-access");
            when(tokenProvider.generateRefreshToken()).thenReturn("new-refresh");
            when(jwtProperties.accessTokenExpiration()).thenReturn(900_000L);
            when(jwtProperties.refreshTokenExpiration()).thenReturn(604_800_000L);
            when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            var result = authService.refresh(request);

            assertThat(result.accessToken()).isEqualTo("new-access");
            assertThat(result.refreshToken()).isEqualTo("new-refresh");
            assertThat(oldToken.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("lanca InvalidTokenException quando o token e consumido concorrentemente")
        void shouldThrowWhenTokenWasConsumedConcurrently() {
            var request = new RefreshRequest("racing-token");
            var user = makeUser(1L, "Ana", "ana@email.com");
            var token = new RefreshToken("racing-token", user,
                Instant.now().plusSeconds(3600));

            when(refreshTokenRepository.findByToken("racing-token"))
                .thenReturn(Optional.of(token));
            when(refreshTokenRepository.revokeIfActive(eq("racing-token"), any(Instant.class)))
                .thenReturn(0);

            assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token has been revoked");
        }

        @Test
        @DisplayName("lanca InvalidTokenException quando token nao existe")
        void shouldThrowWhenTokenNotFound() {
            var request = new RefreshRequest("nonexistent");
            when(refreshTokenRepository.findByToken("nonexistent"))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("lanca InvalidTokenException quando token ja foi revogado")
        void shouldThrowWhenTokenRevoked() {
            var request = new RefreshRequest("revoked-token");
            var user = makeUser(1L, "Ana", "ana@email.com");
            var revokedToken = new RefreshToken("revoked-token", user,
                Instant.now().plusSeconds(3600));
            revokedToken.revoke();

            when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(revokedToken));

            assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(InvalidTokenException.class);
        }
    }

    // ─── logout ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("revoga o refresh token")
        void shouldRevokeToken() {
            var request = new RefreshRequest("my-token");
            var user = makeUser(1L, "Ana", "ana@email.com");
            var token = new RefreshToken("my-token", user,
                Instant.now().plusSeconds(3600));

            when(refreshTokenRepository.findByToken("my-token"))
                .thenReturn(Optional.of(token));

            authService.logout(request);

            assertThat(token.isRevoked()).isTrue();
        }

        @Test
        @DisplayName("nao lanca excecao quando token nao existe")
        void shouldNotThrowWhenTokenNotFound() {
            var request = new RefreshRequest("nonexistent");
            when(refreshTokenRepository.findByToken("nonexistent"))
                .thenReturn(Optional.empty());

            assertThatCode(() -> authService.logout(request))
                .doesNotThrowAnyException();
        }
    }
}
