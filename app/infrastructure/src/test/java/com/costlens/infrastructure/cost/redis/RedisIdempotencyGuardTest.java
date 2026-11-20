package com.costlens.infrastructure.cost.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/**
 * Lua 幂等闸门的契约：脚本返回 1 表示占位成功，0/null 一律视为失败。
 */
@ExtendWith(MockitoExtension.class)
class RedisIdempotencyGuardTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private RedisIdempotencyGuard guard;

    @Test
    @DisplayName("首次占位返回 true")
    void firstAcquireSucceeds() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .willReturn(1L);

        assertThat(guard.tryAcquire("recalc:shard:B1:0", Duration.ofHours(6))).isTrue();
    }

    @Test
    @DisplayName("键已存在时返回 false")
    void duplicateAcquireFails() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .willReturn(0L);

        assertThat(guard.tryAcquire("recalc:shard:B1:0", Duration.ofHours(6))).isFalse();
    }

    @Test
    @DisplayName("Redis 返回 null 按失败处理，不放行")
    void nullResultTreatedAsFailure() {
        given(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .willReturn(null);

        assertThat(guard.tryAcquire("recalc:shard:B1:1", Duration.ofHours(6))).isFalse();
    }

    @Test
    @DisplayName("空键与非法 TTL 直接拒绝")
    void invalidArgumentsRejected() {
        assertThatThrownBy(() -> guard.tryAcquire(" ", Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guard.tryAcquire("k", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }
}