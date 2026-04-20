package com.taskapi.service;

import com.taskapi.dto.UserDTO.*;
import com.taskapi.entity.User;
import com.taskapi.exception.EmailAlreadyExistsException;
import com.taskapi.exception.UserNotFoundException;
import com.taskapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TESTES UNITÁRIOS do UserService.
 *
 * Unitário = testa apenas a lógica do Service, isolado do banco.
 * O @Mock faz o repositório retornar o que você mandar — sem banco real.
 *
 * Estrutura de cada teste:
 *   ARRANGE — prepara os dados e o comportamento dos mocks
 *   ACT     — executa o método que está sendo testado
 *   ASSERT  — verifica se o resultado é o esperado
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    UserRepository repository;      // mock — não acessa banco nenhum

    @InjectMocks
    UserService service;            // recebe o mock acima automaticamente

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User makeUser(Long id, String name, String email) {
        // Reflexão para setar o id (campo privado sem setter)
        try {
            var user = new User(name, email);
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ─── findAll ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("retorna lista de todos os usuários")
        void shouldReturnAllUsers() {
            // ARRANGE
            var users = List.of(
                makeUser(1L, "Ana",   "ana@email.com"),
                makeUser(2L, "Bruno", "bruno@email.com")
            );
            when(repository.findAll()).thenReturn(users);

            // ACT
            var result = service.findAll();

            // ASSERT
            assertThat(result).hasSize(2);
            assertThat(result.get(0).name()).isEqualTo("Ana");
            assertThat(result.get(1).name()).isEqualTo("Bruno");
        }

        @Test
        @DisplayName("retorna lista vazia quando não há usuários")
        void shouldReturnEmptyList() {
            when(repository.findAll()).thenReturn(List.of());

            var result = service.findAll();

            assertThat(result).isEmpty();
        }
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("retorna usuário quando id existe")
        void shouldReturnUserWhenExists() {
            // ARRANGE
            var user = makeUser(1L, "Ana", "ana@email.com");
            when(repository.findById(1L)).thenReturn(Optional.of(user));

            // ACT
            var result = service.findById(1L);

            // ASSERT
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Ana");
            assertThat(result.email()).isEqualTo("ana@email.com");
        }

        @Test
        @DisplayName("lança UserNotFoundException quando id não existe")
        void shouldThrowWhenNotFound() {
            // ARRANGE
            when(repository.findById(99L)).thenReturn(Optional.empty());

            // ASSERT — verifica que a exceção é lançada com a mensagem certa
            assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("99");
        }
    }

    // ─── create ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("cria e retorna usuário quando email não existe")
        void shouldCreateUserWhenEmailNotExists() {
            // ARRANGE
            var request = new CreateUserRequest("Carlos", "carlos@email.com");
            var saved   = makeUser(1L, "Carlos", "carlos@email.com");

            when(repository.existsByEmail("carlos@email.com")).thenReturn(false);
            when(repository.save(any(User.class))).thenReturn(saved);

            // ACT
            var result = service.create(request);

            // ASSERT
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Carlos");

            // Verifica que o repositório foi chamado corretamente
            verify(repository).existsByEmail("carlos@email.com");
            verify(repository).save(any(User.class));
        }

        @Test
        @DisplayName("lança EmailAlreadyExistsException quando email já existe")
        void shouldThrowWhenEmailAlreadyExists() {
            // ARRANGE
            var request = new CreateUserRequest("Carlos", "carlos@email.com");
            when(repository.existsByEmail("carlos@email.com")).thenReturn(true);

            // ASSERT
            assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("carlos@email.com");

            // Garante que NUNCA chamou save() quando email já existia
            verify(repository, never()).save(any());
        }
    }

    // ─── delete ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deleta usuário quando id existe")
        void shouldDeleteWhenExists() {
            when(repository.existsById(1L)).thenReturn(true);

            service.delete(1L);

            // Verifica que deleteById foi chamado com o id correto
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("lança UserNotFoundException quando id não existe")
        void shouldThrowWhenNotFound() {
            when(repository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(UserNotFoundException.class);

            // Garante que deleteById NUNCA foi chamado
            verify(repository, never()).deleteById(any());
        }
    }
}
