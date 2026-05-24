package com.gateway.api_gateway.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RateLimiterService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;
    private final ConcurrentHashMap<String, LocalBucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // Lua script for token bucket with interval-based refill.
        String script =
                "local key = KEYS[1]\n" +
                        "local capacity = tonumber(ARGV[1])\n" +
                "local refill_interval = tonumber(ARGV[2])\n" +
                "local refill_amount = tonumber(ARGV[3])\n" +
                "local now = tonumber(ARGV[4])\n" +
                        "local bucket = redis.call('hmget', key, 'tokens', 'last_refill')\n" +
                        "local tokens = tonumber(bucket[1]) or capacity\n" +
                        "local last_refill = tonumber(bucket[2]) or now\n" +
                        "local delta = math.max(0, now - last_refill)\n" +
                "local intervals = 0\n" +
                "if refill_interval > 0 then\n" +
                "    intervals = math.floor(delta / refill_interval)\n" +
                "end\n" +
                "local refill = intervals * refill_amount\n" +
                        "tokens = math.min(capacity, tokens + refill)\n" +
                "if intervals > 0 then\n" +
                "    last_refill = last_refill + (intervals * refill_interval)\n" +
                "end\n" +
                        "if tokens >= 1 then\n" +
                        "    tokens = tokens - 1\n" +
                "    redis.call('hmset', key, 'tokens', tokens, 'last_refill', last_refill)\n" +
                        "    return 1\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";
        rateLimitScript = new DefaultRedisScript<>(script, Long.class);
    }

    public boolean allowRequest(String key, int capacity, int refillIntervalSeconds, int refillAmount) {
        try {
            Long result = redisTemplate.execute(
                    rateLimitScript,
                    List.of(key),
                    String.valueOf(capacity),
                    String.valueOf(refillIntervalSeconds),
                    String.valueOf(refillAmount),
                    String.valueOf(System.currentTimeMillis() / 1000)
            );
            return result != null && result == 1L;
        } catch (Exception ex) {
            return allowRequestLocally(key, capacity, refillIntervalSeconds, refillAmount);
        }
    }

    public BucketState getBucketState(String key, int capacity, int refillIntervalSeconds, int refillAmount) {
        long now = System.currentTimeMillis() / 1000;
        try {
            Map<Object, Object> bucket = redisTemplate.opsForHash().entries(key);

            int tokens = parseInt(bucket.get("tokens"), capacity);
            long lastRefill = parseLong(bucket.get("last_refill"), now);
            tokens = Math.min(tokens, capacity);

            long delta = Math.max(0, now - lastRefill);
            long intervals = refillIntervalSeconds > 0 ? (delta / refillIntervalSeconds) : 0;
            if (intervals > 0) {
                long refill = intervals * (long) refillAmount;
                tokens = (int) Math.min(capacity, tokens + refill);
                lastRefill = lastRefill + (intervals * refillIntervalSeconds);
                redisTemplate.opsForHash().put(key, "tokens", String.valueOf(tokens));
                redisTemplate.opsForHash().put(key, "last_refill", String.valueOf(lastRefill));
            }

            long secondsUntilNextRefill;
            if (tokens >= capacity || refillIntervalSeconds <= 0) {
                secondsUntilNextRefill = refillIntervalSeconds;
            } else {
                long elapsedSinceLastRefill = Math.max(0, now - lastRefill);
                secondsUntilNextRefill = refillIntervalSeconds - (elapsedSinceLastRefill % refillIntervalSeconds);
            }

            return new BucketState(tokens, capacity, refillIntervalSeconds, refillAmount, secondsUntilNextRefill);
        } catch (Exception ex) {
            return getLocalBucketState(key, capacity, refillIntervalSeconds, refillAmount);
        }
    }

    public void resetBucket(String key, int capacity) {
        long now = System.currentTimeMillis() / 1000;
        try {
            redisTemplate.opsForHash().put(key, "tokens", String.valueOf(capacity));
            redisTemplate.opsForHash().put(key, "last_refill", String.valueOf(now));
        } catch (Exception ignored) {
            localBuckets.put(key, new LocalBucket(capacity, now));
        }
    }

    private boolean allowRequestLocally(String key, int capacity, int refillIntervalSeconds, int refillAmount) {
        long now = System.currentTimeMillis() / 1000;
        LocalBucket bucket = localBuckets.computeIfAbsent(key, ignored -> new LocalBucket(capacity, now));
        synchronized (bucket) {
            bucket.capacity = capacity;
            refill(bucket, now, refillIntervalSeconds, refillAmount);
            if (bucket.tokens >= 1) {
                bucket.tokens -= 1;
                return true;
            }
            return false;
        }
    }

    private BucketState getLocalBucketState(String key, int capacity, int refillIntervalSeconds, int refillAmount) {
        long now = System.currentTimeMillis() / 1000;
        LocalBucket bucket = localBuckets.computeIfAbsent(key, ignored -> new LocalBucket(capacity, now));
        synchronized (bucket) {
            bucket.capacity = capacity;
            refill(bucket, now, refillIntervalSeconds, refillAmount);
            long secondsUntilNextRefill;
            if (bucket.tokens >= bucket.capacity || refillIntervalSeconds <= 0) {
                secondsUntilNextRefill = refillIntervalSeconds;
            } else {
                long elapsedSinceLastRefill = Math.max(0, now - bucket.lastRefill);
                secondsUntilNextRefill = refillIntervalSeconds - (elapsedSinceLastRefill % refillIntervalSeconds);
            }
            return new BucketState(bucket.tokens, capacity, refillIntervalSeconds, refillAmount, secondsUntilNextRefill);
        }
    }

    private void refill(LocalBucket bucket, long now, int refillIntervalSeconds, int refillAmount) {
        long delta = Math.max(0, now - bucket.lastRefill);
        long intervals = refillIntervalSeconds > 0 ? delta / refillIntervalSeconds : 0;
        if (intervals > 0) {
            long refill = intervals * (long) refillAmount;
            bucket.tokens = (int) Math.min(bucket.capacity, bucket.tokens + refill);
            bucket.lastRefill = bucket.lastRefill + (intervals * refillIntervalSeconds);
        }
    }

    private int parseInt(Object value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private long parseLong(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    public record BucketState(
            int availableTokens,
            int capacity,
            int refillIntervalSeconds,
            int refillAmount,
            long secondsUntilNextRefill
    ) {
    }

    private static final class LocalBucket {
        private int tokens;
        private int capacity;
        private long lastRefill;

        private LocalBucket(int capacity, long now) {
            this.tokens = capacity;
            this.capacity = capacity;
            this.lastRefill = now;
        }
    }
}