package com.taskapi.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.dto.TaskDTO.CreateTaskRequest;
import com.taskapi.dto.TaskDTO.TaskResponse;
import com.taskapi.entity.Task;
import com.taskapi.repository.RefreshTokenRepository;
import com.taskapi.repository.TaskRepository;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.AuthDTO.LoginRequest;
import com.taskapi.security.auth.AuthDTO.RefreshRequest;
import com.taskapi.security.auth.AuthDTO.RegisterRequest;
import com.taskapi.security.auth.AuthDTO.TokenResponse;
import com.taskapi.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Auth integration flow")
class AuthIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("registers, logs in, accesses a protected endpoint, refreshes and logs out")
    void shouldCompleteFullAuthFlow() throws Exception {
        String email = "ana+" + System.nanoTime() + "@email.com";
        var registerRequest = new RegisterRequest("Ana Integration", email, "Senha@123");

        var registerResponse = executeTokenRequest("/api/v1/auth/register", registerRequest, status().isCreated());
        assertThat(registerResponse.accessToken()).isNotBlank();
        assertThat(registerResponse.refreshToken()).isNotBlank();

        var loginRequest = new LoginRequest(email, "Senha@123");
        var loginResponse = executeTokenRequest("/api/v1/auth/login", loginRequest, status().isOk());
        Long userId = jwtTokenProvider.getUserIdFromToken(loginResponse.accessToken());

        var createTaskRequest = new CreateTaskRequest(
            "Integration flow task",
            "Created during auth integration test",
            Task.Priority.HIGH,
            LocalDate.now().plusDays(7)
        );

        var createdTask = executeAuthorizedTaskCreation(userId, loginResponse.accessToken(), createTaskRequest);

        mockMvc.perform(get("/api/v1/tasks/{id}", createdTask.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(loginResponse.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(createdTask.id()))
            .andExpect(jsonPath("$.userId").value(userId));

        var refreshedTokens = executeTokenRequest(
            "/api/v1/auth/refresh",
            new RefreshRequest(loginResponse.refreshToken()),
            status().isOk()
        );

        mockMvc.perform(get("/api/v1/tasks/{id}", createdTask.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(refreshedTokens.accessToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Integration flow task"));

        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshedTokens.refreshToken()))))
            .andExpect(status().isNoContent());

        var storedRefreshToken = refreshTokenRepository.findByToken(refreshedTokens.refreshToken());
        assertThat(storedRefreshToken).isPresent();
        assertThat(storedRefreshToken.get().isRevoked()).isTrue();

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(refreshedTokens.refreshToken()))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Refresh token has been revoked"));
    }

    private TokenResponse executeTokenRequest(String path, Object requestBody,
                                              org.springframework.test.web.servlet.ResultMatcher expectedStatus)
            throws Exception {
        var response = mockMvc.perform(post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
            .andExpect(expectedStatus)
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readValue(response, TokenResponse.class);
    }

    private TaskResponse executeAuthorizedTaskCreation(Long userId, String accessToken,
                                                       CreateTaskRequest request) throws Exception {
        var response = mockMvc.perform(post("/api/v1/users/{userId}/tasks", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readValue(response, TaskResponse.class);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
