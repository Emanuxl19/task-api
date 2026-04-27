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

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column
    private String providerId;

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

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters
    public Long getId()                  { return id; }
    public String getName()              { return name; }
    public String getEmail()             { return email; }
    public String getPassword()          { return password; }
    public Role getRole()                { return role; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public String getProviderId()        { return providerId; }
    public List<Task> getTasks()         { return tasks; }

    // Setters
    public void setName(String name)                   { this.name = name; }
    public void setEmail(String email)                 { this.email = email; }
    public void setPassword(String password)            { this.password = password; }
    public void setRole(Role role)                      { this.role = role; }
    public void setAuthProvider(AuthProvider provider)   { this.authProvider = provider; }
    public void setProviderId(String providerId)         { this.providerId = providerId; }
}
