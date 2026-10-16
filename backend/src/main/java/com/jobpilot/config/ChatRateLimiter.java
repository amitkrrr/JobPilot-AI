package com.jobpilot.config;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Sliding-window rate limiter backed by Redis.
 *
 * Key pattern: "ratelimit:{scope}:{identifier}"
 * Uses INCR + EXPIRE: first call in a window sets the counter and the TTL;
 * subsequent calls in the same window only increment.
 */
@Component
public class ChatRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ChatRateLimiter.class);

    private final RedisTemplate<String, Long> redisTemplate;
    private final int windowSeconds;
    private final int maxRequests;

    public ChatRateLimiter(
            RedisTemplate<String, Long> redisTemplate,
            @Value("${cache.rate-limit.window-seconds:60}") int windowSeconds,
            @Value("${cache.rate-limit.max-requests:20}") int maxRequests) {
        this.redisTemplate = redisTemplate;
        this.windowSeconds = windowSeconds;
        this.maxRequests = maxRequests;
    }

    /**
     * Returns true if the caller is allowed (under limit), false if rate-limited.
     * @param userEmail unique identifier for the rate-limit bucket
     */
    public boolean isAllowed(String userEmail) {
        String key = "ratelimit:chat:" + userEmail;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                // First request in this window — set the expiry
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
            }
            if (count != null && count > maxRequests) {
                log.warn("Rate limit exceeded for user={} count={}", userEmail, count);
                return false;
            }
            return true;
        } catch (Exception e) {
            // Redis unavailable — fail open so the app keeps working
            log.warn("Rate limiter Redis error (failing open): {}", e.getMessage());
            return true;
        }
    }

    /** Returns remaining requests in the current window, or maxRequests if Redis is down. */
    public int remaining(String userEmail) {
        String key = "ratelimit:chat:" + userEmail;
        try {
            Long count = redisTemplate.opsForValue().get(key);
            return count == null ? maxRequests : Math.max(0, maxRequests - count.intValue());
        } catch (Exception e) {
            return maxRequests;
        }
    }
}
