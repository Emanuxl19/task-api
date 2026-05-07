package com.taskapi.security.ratelimit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;

@Configuration
class RateLimitConfig {

    @Bean
    Clock rateLimitClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    RateLimitStore redisRateLimitStore(StringRedisTemplate redisTemplate, Clock rateLimitClock) {
        return new RedisRateLimitStore(redisTemplate, rateLimitClock);
    }

    @Bean
    @ConditionalOnMissingBean(RateLimitStore.class)
    RateLimitStore inMemoryRateLimitStore(Clock rateLimitClock) {
        return new InMemoryRateLimitStore(rateLimitClock);
    }
}
