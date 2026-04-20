package com.taskapi.repository;

import com.taskapi.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Busca paginada por usuário
    Page<Task> findByUserId(Long userId, Pageable pageable);

    // Filtra por status
    Page<Task> findByUserIdAndStatus(Long userId, Task.Status status, Pageable pageable);

    // Filtra por prioridade
    Page<Task> findByUserIdAndPriority(Long userId, Task.Priority priority, Pageable pageable);

    // Tarefas vencidas (dueDate < hoje, status != DONE)
    @Query("SELECT t FROM Task t WHERE t.user.id = :userId " +
           "AND t.dueDate < :today AND t.status != 'DONE'")
    List<Task> findOverdueTasks(@Param("userId") Long userId,
                                @Param("today") LocalDate today);

    // Contagem por status — útil para dashboard
    long countByUserIdAndStatus(Long userId, Task.Status status);
}
