package com.taskapi.security.ratelimit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@EnabledIf("isDockerAvailable")
@DisplayName("RedisRateLimitStore (integration)")
class RedisRateLimitStoreIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private LettuceConnectionFactory factory;
    private RedisRateLimitStore store;
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        factory = new LettuceConnectionFactory(
            REDIS.getHost(), REDIS.getMappedPort(6379)
        );
        factory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(factory);
        store = new RedisRateLimitStore(redisTemplate, Clock.systemUTC());

        var connection = factory.getConnection();
        connection.serverCommands().flushDb();
        connection.close();
    }

    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.destroy();
        }
    }

    @Test
    @DisplayName("permite requisicoes dentro do limite")
    void shouldAllowRequestsWithinLimit() {
        for (int i = 0; i < 5; i++) {
            assertThat(store.tryAcquire("test:key1", 5)).isTrue();
        }
    }

    @Test
    @DisplayName("rejeita requisicoes acima do limite")
    void shouldRejectRequestsAboveLimit() {
        for (int i = 0; i < 5; i++) {
            store.tryAcquire("test:key2", 5);
        }

        assertThat(store.tryAcquire("test:key2", 5)).isFalse();
    }

    @Test
    @DisplayName("chaves diferentes sao independentes")
    void shouldIsolateKeys() {
        for (int i = 0; i < 5; i++) {
            store.tryAcquire("test:keyA", 5);
        }

        assertThat(store.tryAcquire("test:keyA", 5)).isFalse();
        assertThat(store.tryAcquire("test:keyB", 5)).isTrue();
    }

    @Test
    @DisplayName("script Lua armazena entradas no sorted set")
    void shouldStoreEntriesInSortedSet() {
        store.tryAcquire("test:verify", 10);
        store.tryAcquire("test:verify", 10);
        store.tryAcquire("test:verify", 10);

        Long size = redisTemplate.opsForZSet().zCard("rate_limit:test:verify");
        assertThat(size).isEqualTo(3);
    }

    @Test
    @DisplayName("chave Redis tem TTL configurado")
    void shouldSetTtlOnKey() {
        store.tryAcquire("test:ttl", 10);

        Long ttl = redisTemplate.getExpire("rate_limit:test:ttl");
        assertThat(ttl).isNotNull().isGreaterThan(0);
    }

    @Test
    @DisplayName("operacao atomica com chamadas concorrentes")
    void shouldBeAtomicUnderConcurrency() throws InterruptedException {
        int limit = 5;
        int threads = 20;
        var latch = new java.util.concurrent.CountDownLatch(threads);
        var successCount = new java.util.concurrent.atomic.AtomicInteger();

        for (int i = 0; i < threads; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    if (store.tryAcquire("test:concurrent", limit)) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        assertThat(successCount.get()).isEqualTo(limit);
    }

    static boolean isDockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
