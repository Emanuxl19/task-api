package com.taskapi.security.auth;

import com.taskapi.exception.InvalidTokenException;
import com.taskapi.repository.RefreshTokenRepository;
import com.taskapi.repository.TaskRepository;
import com.taskapi.repository.UserRepository;
import com.taskapi.security.auth.AuthDTO.RefreshRequest;
import com.taskapi.security.auth.AuthDTO.RegisterRequest;
import com.taskapi.security.auth.AuthDTO.TokenResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("Refresh token concurrency")
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired
    AuthService authService;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @Autowired
    TaskRepository taskRepository;

    @Autowired
    UserRepository userRepository;

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        taskRepository.deleteAll();
        userRepository.deleteAll();
        executor = Executors.newFixedThreadPool(8);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("only one concurrent refresh request may rotate the same token")
    void shouldAllowOnlyOneSuccessfulRefreshForTheSameToken() throws Exception {
        String email = "race+" + System.nanoTime() + "@email.com";
        TokenResponse registered = authService.register(
            new RegisterRequest("Race Condition", email, "Senha@123")
        );

        int attempts = 8;
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RefreshOutcome>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);

                try {
                    TokenResponse refreshed = authService.refresh(
                        new RefreshRequest(registered.refreshToken())
                    );
                    return RefreshOutcome.success(refreshed);
                } catch (Exception ex) {
                    return RefreshOutcome.failure(ex);
                }
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<RefreshOutcome> outcomes = new ArrayList<>();
        for (Future<RefreshOutcome> future : futures) {
            outcomes.add(future.get(5, TimeUnit.SECONDS));
        }

        long successCount = outcomes.stream().filter(RefreshOutcome::successful).count();
        List<Throwable> failures = outcomes.stream()
            .map(RefreshOutcome::failure)
            .filter(failure -> failure != null)
            .toList();

        assertThat(successCount).isEqualTo(1);
        assertThat(failures).hasSize(attempts - 1);
        assertThat(failures)
            .allMatch(failure -> failure instanceof InvalidTokenException)
            .allMatch(failure -> "Refresh token has been revoked".equals(failure.getMessage()));

        var originalToken = refreshTokenRepository.findByToken(registered.refreshToken());
        assertThat(originalToken).isPresent();
        assertThat(originalToken.get().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    private record RefreshOutcome(TokenResponse response, Throwable failure) {
        static RefreshOutcome success(TokenResponse response) {
            return new RefreshOutcome(response, null);
        }

        static RefreshOutcome failure(Throwable failure) {
            return new RefreshOutcome(null, failure);
        }

        boolean successful() {
            return response != null;
        }
    }
}
