package com.taskapi.service;

import com.taskapi.dto.TaskDTO.*;
import com.taskapi.entity.Task;
import com.taskapi.exception.TaskNotFoundException;
import com.taskapi.exception.UserNotFoundException;
import com.taskapi.repository.TaskRepository;
import com.taskapi.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Paginação — essencial em qualquer API real
    public Page<TaskResponse> findByUser(Long userId, Task.Status status,
                                          Task.Priority priority, Pageable pageable) {
        // Streams + pattern matching para decidir qual query executar
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

    public TaskResponse findById(Long id) {
        return taskRepository.findById(id)
            .map(TaskResponse::from)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }

    // Tarefas vencidas — demonstra query customizada
    public List<TaskResponse> findOverdue(Long userId) {
        return taskRepository.findOverdueTasks(userId, LocalDate.now())
            .stream()
            .map(TaskResponse::from)
            .toList();
    }

    @Transactional
    public TaskResponse create(Long userId, CreateTaskRequest request) {
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
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        var task = taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));

        if (request.title() != null)       task.setTitle(request.title());
        if (request.description() != null) task.setDescription(request.description());
        if (request.status() != null)      task.setStatus(request.status());
        if (request.priority() != null)    task.setPriority(request.priority());
        if (request.dueDate() != null)     task.setDueDate(request.dueDate());

        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
    }
}
