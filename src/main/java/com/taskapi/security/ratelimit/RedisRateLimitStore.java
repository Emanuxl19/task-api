package com.taskapi.security.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.util.List;

/**
 * Redis-backed sliding-window rate limiter using sorted sets.
 * Each request is stored as a member with score = timestamp in millis.
 * A Lua script ensures atomicity: cleanup + count + add + ttl in a single call.
 */
public class RedisRateLimitStore implements RateLimitStore {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final String KEY_PREFIX = "rate_limit:";

    /**
     * Lua script executed atomically on Redis:
     * 1. Remove expired entries (score <= cutoff)
     * 2. Count remaining entries
     * 3. If under limit: add new entry with score=now, set TTL=61s
     * 4. Return 1 (allowed) or 0 (denied)
     */
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of(
        """
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local window = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])
        local cutoff = now - window

        redis.call('ZREMRANGEBYSCORE', key, 0, cutoff)

        local count = redis.call('ZCARD', key)
        if count < limit then
            redis.call('ZADD', key, now, now .. ':' .. math.random(1000000))
            redis.call('PEXPIRE', key, window + 1000)
            return 1
        end

        return 0
        """,
        Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public RedisRateLimitStore(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    @Override
    public boolean tryAcquire(String key, int limit) {
        String redisKey = KEY_PREFIX + key;
        long now = clock.millis();

        Long result = redisTemplate.execute(
            RATE_LIMIT_SCRIPT,
            List.of(redisKey),
            String.valueOf(now),
            String.valueOf(WINDOW_MILLIS),
            String.valueOf(limit)
        );

        return result != null && result == 1L;
    }
}
