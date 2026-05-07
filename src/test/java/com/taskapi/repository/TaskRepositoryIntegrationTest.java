package com.taskapi.repository;

import com.taskapi.entity.Task;
import com.taskapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TESTE DE INTEGRAÇÃO com Testcontainers.
 *
 * O que é diferente aqui:
 * - @DataJpaTest sobe apenas a camada JPA (repository + entidades)
 * - @Testcontainers sobe um PostgreSQL REAL em Docker
 * - @ServiceConnection conecta o Spring ao container automaticamente
 * - NÃO usa H2, NÃO usa banco em memória fake
 *
 * Por que isso importa:
 * H2 se comporta diferente do PostgreSQL em queries complexas,
 * constraints, tipos de dados e funções nativas.
 * Testcontainers garante que o que funciona no teste funciona em produção.
 *
 * PRÉ-REQUISITO: Docker rodando na sua máquina.
 */
@DataJpaTest
@Testcontainers
@EnabledIf("isDockerAvailable")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("TaskRepository — integração com PostgreSQL")
class TaskRepositoryIntegrationTest {

    // @ServiceConnection conecta automaticamente ao Spring Boot
    // Sem precisar de @DynamicPropertySource manual
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        userRepository.deleteAll();

        // Cria um usuário base para os testes
        user = userRepository.save(new User("Ana", "ana@email.com"));
    }

    // ─── findByUserId ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByUserId()")
    class FindByUserId {

        @Test
        @DisplayName("retorna tarefas do usuário com paginação")
        void shouldReturnPagedTasksForUser() {
            // ARRANGE — cria 3 tarefas para o usuário
            taskRepository.save(new Task("Tarefa 1", null,
                Task.Priority.HIGH, LocalDate.now().plusDays(5), user));
            taskRepository.save(new Task("Tarefa 2", null,
                Task.Priority.LOW,  LocalDate.now().plusDays(3), user));
            taskRepository.save(new Task("Tarefa 3", null,
                Task.Priority.MEDIUM, LocalDate.now().plusDays(1), user));

            // ACT — page 0, tamanho 2
            var page = taskRepository.findByUserId(user.getId(), PageRequest.of(0, 2));

            // ASSERT
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).hasSize(2);       // página de tamanho 2
            assertThat(page.getTotalPages()).isEqualTo(2);  // 3 itens / 2 = 2 páginas
        }

        @Test
        @DisplayName("não retorna tarefas de outro usuário")
        void shouldNotReturnOtherUsersTask() {
            // ARRANGE — cria outro usuário com uma tarefa
            var outroUsuario = userRepository.save(new User("Bruno", "bruno@email.com"));
            taskRepository.save(new Task("Tarefa do Bruno", null,
                Task.Priority.LOW, LocalDate.now().plusDays(1), outroUsuario));

            // ACT — busca tarefas da Ana
            var page = taskRepository.findByUserId(user.getId(), PageRequest.of(0, 10));

            // ASSERT — Ana não tem tarefas
            assertThat(page.getContent()).isEmpty();
        }
    }

    // ─── findOverdueTasks ────────────────────────────────────────────────────

    @Nested
    @DisplayName("findOverdueTasks()")
    class FindOverdueTasks {

        @Test
        @DisplayName("retorna apenas tarefas vencidas e não concluídas")
        void shouldReturnOnlyOverdueAndNotDone() {
            // ARRANGE
            // Tarefa vencida (dueDate no passado, status TODO) — deve aparecer
            var vencida = taskRepository.save(new Task(
                "Vencida", null, Task.Priority.HIGH,
                LocalDate.now().minusDays(2), user
            ));

            // Tarefa vencida mas já concluída — NÃO deve aparecer
            var concluida = taskRepository.save(new Task(
                "Concluída", null, Task.Priority.LOW,
                LocalDate.now().minusDays(1), user
            ));
            concluida.setStatus(Task.Status.DONE);
            taskRepository.save(concluida);

            // Tarefa futura — NÃO deve aparecer
            taskRepository.save(new Task(
                "Futura", null, Task.Priority.MEDIUM,
                LocalDate.now().plusDays(5), user
            ));

            // ACT
            var overdue = taskRepository.findOverdueTasks(user.getId(), LocalDate.now());

            // ASSERT
            assertThat(overdue).hasSize(1);
            assertThat(overdue.get(0).getTitle()).isEqualTo("Vencida");
        }
    }

    // ─── countByUserIdAndStatus ──────────────────────────────────────────────

    @Nested
    @DisplayName("countByUserIdAndStatus()")
    class CountByStatus {

        @Test
        @DisplayName("conta corretamente por status")
        void shouldCountByStatus() {
            // ARRANGE — 2 TODO, 1 IN_PROGRESS
            taskRepository.save(new Task("T1", null, Task.Priority.LOW,
                LocalDate.now().plusDays(1), user));
            taskRepository.save(new Task("T2", null, Task.Priority.LOW,
                LocalDate.now().plusDays(2), user));

            var inProgress = taskRepository.save(new Task("T3", null,
                Task.Priority.HIGH, LocalDate.now().plusDays(3), user));
            inProgress.setStatus(Task.Status.IN_PROGRESS);
            taskRepository.save(inProgress);

            // ACT + ASSERT
            assertThat(taskRepository.countByUserIdAndStatus(
                user.getId(), Task.Status.TODO)).isEqualTo(2);
            assertThat(taskRepository.countByUserIdAndStatus(
                user.getId(), Task.Status.IN_PROGRESS)).isEqualTo(1);
            assertThat(taskRepository.countByUserIdAndStatus(
                user.getId(), Task.Status.DONE)).isEqualTo(0);
        }
    }

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
