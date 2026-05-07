package com.taskapi.security.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter backed by ConcurrentHashMap.
 * Suitable for single-instance deployments and dev/test environments.
 */
public class InMemoryRateLimitStore implements RateLimitStore {

    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    private final Clock clock;
    private final ConcurrentHashMap<String, Deque<Long>> requestHistory = new ConcurrentHashMap<>();

    public InMemoryRateLimitStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String key, int limit) {
        long now = clock.millis();
        long cutoff = now - WINDOW_MILLIS;
        Deque<Long> timestamps = requestHistory.computeIfAbsent(key, ignored -> new ArrayDeque<>());

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
                timestamps.removeFirst();
            }

            if (timestamps.size() >= limit) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }
}
