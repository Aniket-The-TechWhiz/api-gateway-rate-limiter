package com.gateway.api_gateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RateLimiterService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // Lua script for token bucket with interval-based refill.
        String script =
                "local key = KEYS[1]\n" +
                        "local capacity = tonumber(ARGV[1])\n" +
                "local refill_interval = tonumber(ARGV[2])\n" +
                        "local now = tonumber(ARGV[3])\n" +
                        "local bucket = redis.call('hmget', key, 'tokens', 'last_refill')\n" +
                        "local tokens = tonumber(bucket[1]) or capacity\n" +
                        "local last_refill = tonumber(bucket[2]) or now\n" +
                        "local delta = math.max(0, now - last_refill)\n" +
                "local refill = 0\n" +
                "if refill_interval > 0 then\n" +
                "    refill = math.floor(delta / refill_interval)\n" +
                "end\n" +
                        "tokens = math.min(capacity, tokens + refill)\n" +
                        "if tokens >= 1 then\n" +
                        "    tokens = tokens - 1\n" +
                        "    redis.call('hmset', key, 'tokens', tokens, 'last_refill', now)\n" +
                        "    return 1\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
        rateLimitScript = new DefaultRedisScript<>(script, Long.class);
    }

    public boolean allowRequest(String key, int capacity, int refillPerSecond) {
        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(refillPerSecond),
                    String.valueOf(System.currentTimeMillis() / 1000)
            );
            return result != null && result == 1L;
        } catch (Exception ex) {
            return true;
        }
    }
}