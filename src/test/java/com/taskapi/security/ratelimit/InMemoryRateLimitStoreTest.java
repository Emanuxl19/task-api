package com.taskapi.security.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InMemoryRateLimitStore")
class InMemoryRateLimitStoreTest {

    private MutableClock clock;
    private InMemoryRateLimitStore store;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-04-24T12:00:00Z"), ZoneOffset.UTC);
        store = new InMemoryRateLimitStore(clock);
    }

    @Nested
    @DisplayName("tryAcquire()")
    class TryAcquire {

        @Test
        @DisplayName("permite requisicoes dentro do limite")
        void shouldAllowRequestsWithinLimit() {
            for (int i = 0; i < 5; i++) {
                assertThat(store.tryAcquire("key:1", 5)).isTrue();
            }
        }

        @Test
        @DisplayName("rejeita requisicoes acima do limite")
        void shouldRejectRequestsAboveLimit() {
            for (int i = 0; i < 5; i++) {
                store.tryAcquire("key:1", 5);
            }

            assertThat(store.tryAcquire("key:1", 5)).isFalse();
        }

        @Test
        @DisplayName("chaves diferentes sao independentes")
        void shouldIsolateKeys() {
            for (int i = 0; i < 5; i++) {
                store.tryAcquire("key:1", 5);
            }

            assertThat(store.tryAcquire("key:1", 5)).isFalse();
            assertThat(store.tryAcquire("key:2", 5)).isTrue();
        }

        @Test
        @DisplayName("permite requisicoes apos a janela de 1 minuto expirar")
        void shouldAllowRequestsAfterWindowExpires() {
            for (int i = 0; i < 5; i++) {
                store.tryAcquire("key:1", 5);
            }

            assertThat(store.tryAcquire("key:1", 5)).isFalse();

            clock.advanceSeconds(61);

            assertThat(store.tryAcquire("key:1", 5)).isTrue();
        }

        @Test
        @DisplayName("sliding window limpa apenas entradas expiradas")
        void shouldOnlyExpireOldEntries() {
            // 3 requests at T+0
            for (int i = 0; i < 3; i++) {
                store.tryAcquire("key:1", 5);
            }

            // advance 30s, 2 more requests
            clock.advanceSeconds(30);
            store.tryAcquire("key:1", 5);
            store.tryAcquire("key:1", 5);

            // now at limit=5
            assertThat(store.tryAcquire("key:1", 5)).isFalse();

            // advance to T+61s — the first 3 expire, the 2 at T+30s remain
            clock.advanceSeconds(31);

            assertThat(store.tryAcquire("key:1", 5)).isTrue();
            assertThat(store.tryAcquire("key:1", 5)).isTrue();
            assertThat(store.tryAcquire("key:1", 5)).isTrue();
            // now 5 active (2 old + 3 new)
            assertThat(store.tryAcquire("key:1", 5)).isFalse();
        }

        @Test
        @DisplayName("limite de 1 permite apenas uma requisicao por janela")
        void shouldRespectLimitOfOne() {
            assertThat(store.tryAcquire("strict", 1)).isTrue();
            assertThat(store.tryAcquire("strict", 1)).isFalse();

            clock.advanceSeconds(61);

            assertThat(store.tryAcquire("strict", 1)).isTrue();
        }
    }

    // ─── Clock de teste ────────────────────────────────────────────────

    private static final class MutableClock extends Clock {

        private Instant current;
        private final ZoneId zone;

        private MutableClock(Instant current, ZoneId zone) {
            this.current = current;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }

        void advanceSeconds(long seconds) {
            current = current.plusSeconds(seconds);
        }
    }
}
