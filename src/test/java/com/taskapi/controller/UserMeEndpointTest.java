package com.taskapi.controller;

import com.taskapi.dto.UserDTO.UserResponse;
import com.taskapi.entity.Role;
import com.taskapi.entity.User;
import com.taskapi.exception.GlobalExceptionHandler;
import com.taskapi.security.auth.CustomUserDetails;
import com.taskapi.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("GET /api/v1/users/me")
class UserMeEndpointTest {

    @Mock
    UserService service;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new UserController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("retorna 200 com perfil do usuario autenticado")
    void shouldReturn200WithAuthenticatedUserProfile() throws Exception {
        var principal = authenticate(42L, "Ana", "ana@email.com", Role.USER);

        when(service.findById(42L))
            .thenReturn(new UserResponse(42L, "Ana", "ana@email.com", 5));

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.name").value("Ana"))
            .andExpect(jsonPath("$.email").value("ana@email.com"))
            .andExpect(jsonPath("$.taskCount").value(5));
    }

    @Test
    @DisplayName("retorna perfil com role ADMIN")
    void shouldReturnAdminProfile() throws Exception {
        var principal = authenticate(1L, "Admin", "admin@email.com", Role.ADMIN);

        when(service.findById(1L))
            .thenReturn(new UserResponse(1L, "Admin", "admin@email.com", 0));

        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Admin"));
    }

    private CustomUserDetails authenticate(Long id, String name, String email, Role role) {
        var user = new User(name, email, "secret");
        user.setRole(role);
        setField(User.class, user, "id", id);

        var principal = new CustomUserDetails(user);
        var authentication = new UsernamePasswordAuthenticationToken(
            principal, null, principal.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return principal;
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
}
