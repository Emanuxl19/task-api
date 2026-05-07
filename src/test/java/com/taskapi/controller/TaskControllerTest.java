package com.taskapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.dto.TaskDTO.CreateTaskRequest;
import com.taskapi.dto.TaskDTO.TaskResponse;
import com.taskapi.dto.TaskDTO.UpdateTaskRequest;
import com.taskapi.entity.Role;
import com.taskapi.entity.Task;
import com.taskapi.entity.User;
import com.taskapi.exception.GlobalExceptionHandler;
import com.taskapi.security.auth.CustomUserDetails;
import com.taskapi.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController")
class TaskControllerTest {

    @Mock
    TaskService service;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new TaskController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(
                new AuthenticationPrincipalArgumentResolver(),
                new PageableHandlerMethodArgumentResolver()
            )
            .build();

        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/tasks")
    class FindByUser {

        @Test
        @DisplayName("returns 200 and delegates authenticated principal to service")
        void shouldReturn200AndPassAuthenticatedPrincipal() throws Exception {
            var principal = authenticate(1L, Role.USER);
            var pageRequest = PageRequest.of(0, 10);
            var response = taskResponse(10L, "Finish report", 1L, "Ana");

            when(service.findByUser(eq(principal), eq(1L), eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(response), pageRequest, 1));

            mockMvc.perform(get("/api/v1/users/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].userId").value(1));

            verify(service).findByUser(eq(principal), eq(1L), eq(null), eq(null), any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{userId}/tasks/overdue")
    class FindOverdue {

        @Test
        @DisplayName("returns 403 when service denies access")
        void shouldReturn403WhenAccessIsDenied() throws Exception {
            var principal = authenticate(1L, Role.USER);

            doThrow(new AccessDeniedException("forbidden"))
                .when(service).findOverdue(principal, 2L);

            mockMvc.perform(get("/api/v1/users/2/tasks/overdue"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/tasks/{id}")
    class FindById {

        @Test
        @DisplayName("returns 200 for authorized user")
        void shouldReturn200WhenAuthorized() throws Exception {
            var principal = authenticate(1L, Role.USER);
            var response = taskResponse(15L, "Read docs", 1L, "Ana");

            when(service.findById(principal, 15L)).thenReturn(response);

            mockMvc.perform(get("/api/v1/tasks/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Read docs"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/users/{userId}/tasks")
    class Create {

        @Test
        @DisplayName("returns 201 when payload is valid")
        void shouldReturn201WhenPayloadIsValid() throws Exception {
            var principal = authenticate(1L, Role.USER);
            var request = new CreateTaskRequest(
                "Prepare release",
                "Details",
                Task.Priority.HIGH,
                LocalDate.now().plusDays(3)
            );
            var response = taskResponse(20L, "Prepare release", 1L, "Ana");

            when(service.create(eq(principal), eq(1L), any(CreateTaskRequest.class)))
                .thenReturn(response);

            mockMvc.perform(post("/api/v1/users/1/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.title").value("Prepare release"));

            verify(service).create(eq(principal), eq(1L), any(CreateTaskRequest.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/tasks/{id}")
    class Update {

        @Test
        @DisplayName("returns 200 when payload is valid")
        void shouldReturn200WhenPayloadIsValid() throws Exception {
            var principal = authenticate(1L, Role.USER);
            var request = new UpdateTaskRequest(
                "Updated title",
                "Updated description",
                Task.Status.IN_PROGRESS,
                Task.Priority.HIGH,
                LocalDate.now().plusDays(7)
            );
            var response = taskResponse(25L, "Updated title", 1L, "Ana");

            when(service.update(eq(principal), eq(25L), any(UpdateTaskRequest.class)))
                .thenReturn(response);

            mockMvc.perform(put("/api/v1/tasks/25")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated title"));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{id}")
    class Delete {

        @Test
        @DisplayName("returns 204 when delete succeeds")
        void shouldReturn204WhenDeleteSucceeds() throws Exception {
            var principal = authenticate(1L, Role.USER);

            mockMvc.perform(delete("/api/v1/tasks/30"))
                .andExpect(status().isNoContent());

            verify(service).delete(principal, 30L);
        }
    }

    private CustomUserDetails authenticate(Long userId, Role role) {
        var user = new User("Authenticated", "user@email.com", "secret");
        user.setRole(role);
        setField(User.class, user, "id", userId);

        var principal = new CustomUserDetails(user);
        var authentication = new UsernamePasswordAuthenticationToken(
            principal,
            null,
            principal.getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        return principal;
    }

    private TaskResponse taskResponse(Long id, String title, Long userId, String userName) {
        return new TaskResponse(
            id,
            title,
            "Description",
            Task.Status.TODO,
            Task.Priority.MEDIUM,
            LocalDate.now().plusDays(1),
            LocalDateTime.now(),
            userId,
            userName
        );
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
