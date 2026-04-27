package com.taskapi.service;

import com.taskapi.dto.TaskDTO.*;
import com.taskapi.entity.Role;
import com.taskapi.entity.Task;
import com.taskapi.exception.TaskNotFoundException;
import com.taskapi.exception.UserNotFoundException;
import com.taskapi.repository.TaskRepository;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    // Paginacao essencial em qualquer API real
    public Page<TaskResponse> findByUser(CustomUserDetails currentUser, Long userId,
                                         Task.Status status, Task.Priority priority,
                                         Pageable pageable) {
        validateUserAccess(currentUser, userId);

        if (status != null) {
            return taskRepository
                .findByUserIdAndStatus(userId, status, pageable)
                .map(TaskResponse::from);
        }
        if (priority != null) {
            return taskRepository
                .findByUserIdAndPriority(userId, priority, pageable)
                .map(TaskResponse::from);
        }
        return taskRepository.findByUserId(userId, pageable).map(TaskResponse::from);
    }

    public TaskResponse findById(CustomUserDetails currentUser, Long id) {
        return TaskResponse.from(findAuthorizedTask(currentUser, id));
    }

    // Tarefas vencidas demonstram query customizada
    public List<TaskResponse> findOverdue(CustomUserDetails currentUser, Long userId) {
        validateUserAccess(currentUser, userId);

        return taskRepository.findOverdueTasks(userId, LocalDate.now())
            .stream()
            .map(TaskResponse::from)
            .toList();
    }

    @Transactional
    public TaskResponse create(CustomUserDetails currentUser, Long userId,
                               CreateTaskRequest request) {
        validateUserAccess(currentUser, userId);

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        var task = new Task(
            request.title(),
            request.description(),
            request.priority(),
            request.dueDate(),
            user
        );

        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(CustomUserDetails currentUser, Long id,
                               UpdateTaskRequest request) {
        var task = findAuthorizedTask(currentUser, id);

        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.status() != null) {
            task.setStatus(request.status());
        }
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }

        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(CustomUserDetails currentUser, Long id) {
        var task = findAuthorizedTask(currentUser, id);
        taskRepository.delete(task);
    }

    private Task findAuthorizedTask(CustomUserDetails currentUser, Long taskId) {
        var task = taskRepository.findById(taskId)
            .orElseThrow(() -> new TaskNotFoundException(taskId));

        validateUserAccess(currentUser, task.getUser().getId());
        return task;
    }

    private void validateUserAccess(CustomUserDetails currentUser, Long ownerId) {
        if (isAdmin(currentUser)) {
            return;
        }

        if (!currentUser.getId().equals(ownerId)) {
            throw new AccessDeniedException("You do not have permission to access this task");
        }
    }

    private boolean isAdmin(CustomUserDetails currentUser) {
        return currentUser.getUser().getRole() == Role.ADMIN;
    }
}
