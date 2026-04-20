package com.taskapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskapi.dto.UserDTO.*;
import com.taskapi.exception.EmailAlreadyExistsException;
import com.taskapi.exception.GlobalExceptionHandler;
import com.taskapi.exception.UserNotFoundException;
import com.taskapi.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TESTES DO CONTROLLER com MockMvc.
 *
 * @WebMvcTest — sobe apenas a camada web (controller + filtros)
 * Não sobe o banco, não sobe o service real.
 * O @MockBean substitui o service por um mock.
 *
 * O que esses testes verificam:
 * - Status HTTP correto (200, 201, 404, 409...)
 * - Estrutura do JSON de resposta
 * - Validação de entrada (@Valid)
 * - Tratamento de erros (GlobalExceptionHandler)
 */
@WebMvcTest({UserController.class, GlobalExceptionHandler.class})
@DisplayName("UserController")
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;            // simula requisições HTTP sem subir servidor

    @Autowired
    ObjectMapper objectMapper;  // converte objetos Java <-> JSON

    @MockBean
    UserService service;        // mock do service — você controla o retorno

    // ─── GET /api/v1/users ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/users")
    class GetAll {

        @Test
        @DisplayName("retorna 200 com lista de usuários")
        void shouldReturn200WithUsers() throws Exception {
            // ARRANGE
            var users = List.of(
                new UserResponse(1L, "Ana",   "ana@email.com",   0),
                new UserResponse(2L, "Bruno", "bruno@email.com", 2)
            );
            when(service.findAll()).thenReturn(users);

            // ACT + ASSERT
            mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())                         // 200
                .andExpect(jsonPath("$.length()").value(2))         // 2 itens
                .andExpect(jsonPath("$[0].name").value("Ana"))
                .andExpect(jsonPath("$[1].taskCount").value(2));
        }
    }

    // ─── GET /api/v1/users/{id} ──────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetById {

        @Test
        @DisplayName("retorna 200 quando usuário existe")
        void shouldReturn200WhenFound() throws Exception {
            when(service.findById(1L))
                .thenReturn(new UserResponse(1L, "Ana", "ana@email.com", 0));

            mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Ana"))
                .andExpect(jsonPath("$.email").value("ana@email.com"));
        }

        @Test
        @DisplayName("retorna 404 quando usuário não existe")
        void shouldReturn404WhenNotFound() throws Exception {
            when(service.findById(99L))
                .thenThrow(new UserNotFoundException(99L));

            mockMvc.perform(get("/api/v1/users/99"))
                .andExpect(status().isNotFound())                   // 404
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists());
        }
    }

    // ─── POST /api/v1/users ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/users")
    class Create {

        @Test
        @DisplayName("retorna 201 quando dados são válidos")
        void shouldReturn201WhenValid() throws Exception {
            // ARRANGE
            var request  = new CreateUserRequest("Carlos", "carlos@email.com");
            var response = new UserResponse(1L, "Carlos", "carlos@email.com", 0);

            when(service.create(any(CreateUserRequest.class))).thenReturn(response);

            // ACT + ASSERT
            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())                    // 201
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Carlos"));
        }

        @Test
        @DisplayName("retorna 400 quando nome está em branco")
        void shouldReturn400WhenNameIsBlank() throws Exception {
            var request = new CreateUserRequest("", "carlos@email.com");

            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())                 // 400
                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("retorna 400 quando email é inválido")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            var request = new CreateUserRequest("Carlos", "nao-e-um-email");

            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("retorna 409 quando email já existe")
        void shouldReturn409WhenEmailConflict() throws Exception {
            var request = new CreateUserRequest("Carlos", "carlos@email.com");

            when(service.create(any()))
                .thenThrow(new EmailAlreadyExistsException("carlos@email.com"));

            mockMvc.perform(post("/api/v1/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())                   // 409
                .andExpect(jsonPath("$.status").value(409));
        }
    }

    // ─── DELETE /api/v1/users/{id} ───────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/users/{id}")
    class Delete {

        @Test
        @DisplayName("retorna 204 quando usuário existe")
        void shouldReturn204WhenDeleted() throws Exception {
            mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isNoContent());                 // 204
        }

        @Test
        @DisplayName("retorna 404 quando usuário não existe")
        void shouldReturn404WhenNotFound() throws Exception {
            doThrow(new UserNotFoundException(99L))
                .when(service).delete(99L);

            mockMvc.perform(delete("/api/v1/users/99"))
                .andExpect(status().isNotFound());
        }
    }
}
