package com.taskapi.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_rt_user_revoked", columnList = "user_id, revoked"),
    @Index(name = "idx_rt_expires_at", columnList = "expires_at")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean revoked = false;

    protected RefreshToken() {}

    public RefreshToken(String token, User user, Instant expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    // Getters
    public Long getId()          { return id; }
    public String getToken()     { return token; }
    public User getUser()        { return user; }
    public Instant getExpiresAt(){ return expiresAt; }
    public Instant getCreatedAt(){ return createdAt; }
    public boolean isRevoked()   { return revoked; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
