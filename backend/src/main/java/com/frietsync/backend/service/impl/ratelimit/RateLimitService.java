package com.frietsync.backend.service.impl.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT =
            new DefaultRedisScript<>("""
                    local capacity = tonumber(ARGV[1])
                    local windowMillis = tonumber(ARGV[2])
                    local time = redis.call('TIME')
                    local now = (tonumber(time[1]) * 1000) + math.floor(tonumber(time[2]) / 1000)
                    local bucket = redis.call('HMGET', KEYS[1], 'tokens', 'timestamp')
                    local tokens = tonumber(bucket[1]) or capacity
                    local last = tonumber(bucket[2]) or now
                    local elapsed = math.max(0, now - last)
                    tokens = math.min(capacity, tokens + (elapsed * capacity / windowMillis))
                    local allowed = 0
                    if tokens >= 1 then
                        tokens = tokens - 1
                        allowed = 1
                    end
                    redis.call('HSET', KEYS[1], 'tokens', tokens, 'timestamp', now)
                    redis.call('PEXPIRE', KEYS[1], windowMillis * 2)
                    return allowed
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(String key, int maxRequests, Duration window) {
        if (maxRequests <= 0 || window.isZero() || window.isNegative()) {
            return false;
        }

        Long allowed = redisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                List.of(key),
                Integer.toString(maxRequests),
                Long.toString(window.toMillis())
        );
        return Long.valueOf(1).equals(allowed);
    }
}
