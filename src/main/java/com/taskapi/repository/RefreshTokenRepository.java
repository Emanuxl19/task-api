package com.taskapi.repository;

import com.taskapi.entity.RefreshToken;
import com.taskapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying(flushAutomatically = true)
    @Query("""
        UPDATE RefreshToken rt
           SET rt.revoked = true
         WHERE rt.token = :token
           AND rt.revoked = false
           AND rt.expiresAt > :now
        """)
    int revokeIfActive(@Param("token") String token, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") Instant now);
}
