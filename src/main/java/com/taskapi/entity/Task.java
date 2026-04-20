package com.taskapi.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tasks")
public class Task {

    // Sealed interface — Java 21 — modela os status possíveis de forma segura
    // (você vai aprender mais sobre sealed classes na Fase 1)
    public enum Status {
        TODO, IN_PROGRESS, DONE
    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority = Priority.MEDIUM;

    private LocalDate dueDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Muitas tarefas pertencem a um usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected Task() {}

    public Task(String title, String description, Priority priority,
                LocalDate dueDate, User user) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.user = user;
    }

    // Getters
    public Long getId()             { return id; }
    public String getTitle()        { return title; }
    public String getDescription()  { return description; }
    public Status getStatus()       { return status; }
    public Priority getPriority()   { return priority; }
    public LocalDate getDueDate()   { return dueDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public User getUser()           { return user; }

    // Setters
    public void setTitle(String title)            { this.title = title; }
    public void setDescription(String description){ this.description = description; }
    public void setStatus(Status status)          { this.status = status; }
    public void setPriority(Priority priority)    { this.priority = priority; }
    public void setDueDate(LocalDate dueDate)     { this.dueDate = dueDate; }
}
