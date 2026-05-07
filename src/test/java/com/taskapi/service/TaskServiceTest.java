package com.taskapi.service;

import com.taskapi.dto.TaskDTO.CreateTaskRequest;
import com.taskapi.dto.TaskDTO.TaskResponse;
import com.taskapi.dto.TaskDTO.UpdateTaskRequest;
import com.taskapi.entity.Role;
import com.taskapi.entity.Task;
import com.taskapi.entity.User;
import com.taskapi.exception.UserNotFoundException;
import com.taskapi.repository.TaskRepository;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock
    TaskRepository taskRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    TaskService service;

    private final PageRequest pageable = PageRequest.of(0, 10);

    @Nested
    @DisplayName("findByUser()")
    class FindByUser {

        @Test
        @DisplayName("returns current user tasks when owner requests them")
        void shouldReturnTasksWhenUserOwnsThem() {
            var owner = makeUser(1L, "Ana", "ana@email.com", Role.USER);
            var task = makeTask(10L, "Finish report", owner);
            var principal = new CustomUserDetails(owner);

            when(taskRepository.findByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

            var result = service.findByUser(principal, 1L, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("Finish report");
            verify(taskRepository).findByUserId(1L, pageable);
        }

        @Test
        @DisplayName("throws access denied when user requests another users tasks")
        void shouldThrowWhenUserRequestsAnotherUsersTasks() {
            var principal = new CustomUserDetails(
                makeUser(1L, "Ana", "ana@email.com", Role.USER)
            );

            assertThatThrownBy(() -> service.findByUser(principal, 2L, null, null, pageable))
                .isInstanceOf(AccessDeniedException.class);

            verify(taskRepository, never()).findByUserId(any(), any());
        }

        @Test
        @DisplayName("allows admin to request any users tasks")
        void shouldAllowAdminToRequestAnyUsersTasks() {
            var owner = makeUser(2L, "Bruno", "bruno@email.com", Role.USER);
            var admin = new CustomUserDetails(
                makeUser(99L, "Admin", "admin@email.com", Role.ADMIN)
            );
            var task = makeTask(20L, "Review backlog", owner);

            when(taskRepository.findByUserIdAndStatus(2L, Task.Status.TODO, pageable))
                .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

            var result = service.findByUser(admin, 2L, Task.Status.TODO, null, pageable);

            assertThat(result.getContent()).extracting(TaskResponse::userId).containsExactly(2L);
            verify(taskRepository).findByUserIdAndStatus(2L, Task.Status.TODO, pageable);
        }
    }

    @Nested
    @DisplayName("findOverdue()")
    class FindOverdue {

        @Test
        @DisplayName("throws access denied when overdue list belongs to another user")
        void shouldThrowWhenOverdueListBelongsToAnotherUser() {
            var principal = new CustomUserDetails(
                makeUser(1L, "Ana", "ana@email.com", Role.USER)
            );

            assertThatThrownBy(() -> service.findOverdue(principal, 2L))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("throws access denied when task belongs to another user")
        void shouldThrowWhenTaskBelongsToAnotherUser() {
            var principal = new CustomUserDetails(
                makeUser(1L, "Ana", "ana@email.com", Role.USER)
            );
            var otherOwner = makeUser(2L, "Bruno", "bruno@email.com", Role.USER);
            var task = makeTask(30L, "Private task", otherOwner);

            when(taskRepository.findById(30L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> service.findById(principal, 30L))
                .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("allows admin to read another users task")
        void shouldAllowAdminToReadAnotherUsersTask() {
            var admin = new CustomUserDetails(
                makeUser(99L, "Admin", "admin@email.com", Role.ADMIN)
            );
            var owner = makeUser(2L, "Bruno", "bruno@email.com", Role.USER);
            var task = makeTask(31L, "Escalated task", owner);

            when(taskRepository.findById(31L)).thenReturn(Optional.of(task));

            var result = service.findById(admin, 31L);

            assertThat(result.id()).isEqualTo(31L);
            assertThat(result.userId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("throws access denied when user creates task for another user")
        void shouldThrowWhenUserCreatesTaskForAnotherUser() {
            var principal = new CustomUserDetails(
                makeUser(1L, "Ana", "ana@email.com", Role.USER)
            );
            var request = new CreateTaskRequest(
                "New task",
                "Details",
                Task.Priority.HIGH,
                LocalDate.now().plusDays(3)
            );

            assertThatThrownBy(() -> service.create(principal, 2L, request))
                .isInstanceOf(AccessDeniedException.class);

            verify(userRepository, never()).findById(any());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("allows admin to create task for another user")
        void shouldAllowAdminToCreateTaskForAnotherUser() {
            var admin = new CustomUserDetails(
                makeUser(99L, "Admin", "admin@email.com", Role.ADMIN)
            );
            var owner = makeUser(2L, "Bruno", "bruno@email.com", Role.USER);
            var request = new CreateTaskRequest(
                "Delegated task",
                "Details",
                Task.Priority.MEDIUM,
                LocalDate.now().plusDays(5)
            );
            var savedTask = makeTask(40L, "Delegated task", owner);

            when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
            when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

            var result = service.create(admin, 2L, request);

            assertThat(result.id()).isEqualTo(40L);
            assertThat(result.userId()).isEqualTo(2L);
            verify(userRepository).findById(2L);
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("throws user not found when target user does not exist")
        void shouldThrowWhenTargetUserDoesNotExist() {
            var admin = new CustomUserDetails(
                makeUser(99L, "Admin", "admin@email.com", Role.ADMIN)
            );
            var request = new CreateTaskRequest(
                "Delegated task",
                "Details",
                Task.Priority.MEDIUM,
                LocalDate.now().plusDays(5)
            );

            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(admin, 2L, request))
                .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("throws access denied when user updates another users task")
        void shouldThrowWhenUserUpdatesAnotherUsersTask() {
            var principal = new CustomUserDetails(
                makeUser(1L, "Ana", "ana@email.com", Role.USER)
            );
            var otherOwner = makeUser(2L, "Bruno", "bruno@email.com", Role.USER);
            var task = makeTask(50L, "Private task", otherOwner);
            var request = new UpdateTaskRequest("Updated", null, null, null, null);

            when(taskRepository.findById(50L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> service.update(principal, 50L, request))
                .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deletes task when owner requests it")
        void shouldDeleteWhenOwnerRequestsIt() {
            var owner = makeUser(1L, "Ana", "ana@email.com", Role.USER);
            var principal = new CustomUserDetails(owner);
            var task = makeTask(60L, "Disposable", owner);

            when(taskRepository.findById(60L)).thenReturn(Optional.of(task));

            service.delete(principal, 60L);

            verify(taskRepository).delete(eq(task));
        }
    }

    private User makeUser(Long id, String name, String email, Role role) {
        var user = new User(name, email, "secret");
        user.setRole(role);
        setField(User.class, user, "id", id);
        return user;
    }

    private Task makeTask(Long id, String title, User owner) {
        var task = new Task(
            title,
            "Description",
            Task.Priority.MEDIUM,
            LocalDate.now().plusDays(2),
            owner
        );
        setField(Task.class, task, "id", id);
        return task;
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
