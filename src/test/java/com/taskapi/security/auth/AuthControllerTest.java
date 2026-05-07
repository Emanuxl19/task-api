package com.taskapi.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.exception.EmailAlreadyExistsException;
import com.taskapi.exception.GlobalExceptionHandler;
import com.taskapi.exception.InvalidTokenException;
import com.taskapi.security.auth.AuthDTO.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = {AuthController.class, GlobalExceptionHandler.class},
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, OAuth2ClientAutoConfiguration.class}
)
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AuthService authService;

    // ─── POST /api/v1/auth/register ─────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("retorna 201 com tokens quando dados sao validos")
        void shouldReturn201WithTokens() throws Exception {
            var request = new RegisterRequest("Ana", "ana@email.com", "Senha@123");
            var response = TokenResponse.of("access-token", "refresh-token", 900_000);

            when(authService.register(any(RegisterRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
        }

        @Test
        @DisplayName("retorna 400 quando senha e fraca")
        void shouldReturn400WhenWeakPassword() throws Exception {
            var request = new RegisterRequest("Ana", "ana@email.com", "123");

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("retorna 400 quando email e invalido")
        void shouldReturn400WhenInvalidEmail() throws Exception {
            var request = new RegisterRequest("Ana", "nao-e-email", "Senha@123");

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 409 quando email ja existe")
        void shouldReturn409WhenEmailExists() throws Exception {
            var request = new RegisterRequest("Ana", "ana@email.com", "Senha@123");

            when(authService.register(any()))
                .thenThrow(new EmailAlreadyExistsException("ana@email.com"));

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
        }
    }

    // ─── POST /api/v1/auth/login ────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("retorna 200 com tokens quando credenciais sao validas")
        void shouldReturn200WithTokens() throws Exception {
            var request = new LoginRequest("ana@email.com", "Senha@123");
            var response = TokenResponse.of("access-token", "refresh-token", 900_000);

            when(authService.login(any(LoginRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"));
        }

        @Test
        @DisplayName("retorna 401 quando credenciais sao invalidas")
        void shouldReturn401OnBadCredentials() throws Exception {
            var request = new LoginRequest("ana@email.com", "wrong");

            when(authService.login(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        }
    }

    // ─── POST /api/v1/auth/refresh ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("retorna 200 com novos tokens")
        void shouldReturn200WithNewTokens() throws Exception {
            var request = new RefreshRequest("old-token");
            var response = TokenResponse.of("new-access", "new-refresh", 900_000);

            when(authService.refresh(any(RefreshRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
        }

        @Test
        @DisplayName("retorna 401 quando refresh token e invalido")
        void shouldReturn401OnInvalidToken() throws Exception {
            var request = new RefreshRequest("bad-token");

            when(authService.refresh(any()))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

            mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        }
    }

    // ─── POST /api/v1/auth/logout ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("retorna 204 ao fazer logout")
        void shouldReturn204() throws Exception {
            var request = new RefreshRequest("my-token");

            mockMvc.perform(post("/api/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        }
    }

    // ─── POST /api/v1/auth/oauth2/exchange ──────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/oauth2/exchange")
    class OAuth2Exchange {

        @Test
        @DisplayName("retorna 200 com tokens quando o code e valido")
        void shouldReturn200WithTokens() throws Exception {
            var request = new OAuth2ExchangeRequest("valid-code");
            var response = TokenResponse.of("access-token", "refresh-token", 900_000);

            when(authService.exchangeOAuth2Code(any(OAuth2ExchangeRequest.class)))
                .thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        }

        @Test
        @DisplayName("retorna 400 quando o code esta em branco")
        void shouldReturn400WhenCodeBlank() throws Exception {
            var request = new OAuth2ExchangeRequest("");

            mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 401 quando o code e invalido ou expirou")
        void shouldReturn401WhenCodeInvalid() throws Exception {
            var request = new OAuth2ExchangeRequest("expired-code");

            when(authService.exchangeOAuth2Code(any()))
                .thenThrow(new InvalidTokenException("Invalid or expired OAuth2 authorization code"));

            mockMvc.perform(post("/api/v1/auth/oauth2/exchange")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        }
    }
}
