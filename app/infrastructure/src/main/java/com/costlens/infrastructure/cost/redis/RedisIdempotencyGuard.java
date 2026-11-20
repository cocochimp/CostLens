package com.costlens.infrastructure.cost.redis;

import com.costlens.application.cost.port.IdempotencyGuard;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Redis + Lua 实现的幂等闸门。
 * 用单条 SET key val NX PX ttl 完成「检查 + 占位」，脚本内不存在 check-then-set 窗口。
 */
@Component
public class RedisIdempotencyGuard implements IdempotencyGuard {

    private static final String ACQUIRE_SCRIPT = """
            if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then
              return 1
            else
              return 0
            end
            """;

    private static final DefaultRedisScript<Long> SCRIPT =
            new DefaultRedisScript<>(ACQUIRE_SCRIPT, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("幂等键不能为空");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL 必须为正");
        }
        Long result = redisTemplate.execute(SCRIPT, List.of(key), "1", String.valueOf(ttl.toMillis()));
        return result != null && result == 1L;
    }
}