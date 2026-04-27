package com.taskapi.security.auth;

import com.taskapi.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenCleanupJob")
class RefreshTokenCleanupJobTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    RefreshTokenCleanupJob cleanupJob;

    @Test
    @DisplayName("deleta tokens expirados quando executado")
    void shouldDeleteExpiredTokens() {
        cleanupJob.purgeExpiredTokens();

        verify(refreshTokenRepository).deleteExpiredTokens(any(Instant.class));
    }
}
