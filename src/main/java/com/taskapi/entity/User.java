package com.taskapi.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    // Um usuário tem muitas tarefas
    // orphanRemoval = true: deleta tarefas quando o usuário é deletado
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> tasks = new ArrayList<>();

    // JPA exige construtor sem argumentos
    protected User() {}

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Getters
    public Long getId()          { return id; }
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public List<Task> getTasks() { return tasks; }

    // Setters (só o que pode ser alterado)
    public void setName(String name)   { this.name = name; }
    public void setEmail(String email) { this.email = email; }
}
