package com.taskapi.security.ratelimit;

/**
 * Abstraction for rate-limit sliding-window storage.
 * Implementations can be in-memory (dev) or Redis-backed (production).
 */
public interface RateLimitStore {

    /**
     * Tries to record a request for the given key within a 1-minute sliding window.
     *
     * @param key   identifier (e.g. "login:ip:10.0.0.1" or "ai:user:42")
     * @param limit maximum number of requests allowed in the window
     * @return true if the request is allowed, false if the limit is exceeded
     */
    boolean tryAcquire(String key, int limit);
}
