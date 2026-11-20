package com.costlens.application.cost.port;

import java.time.Duration;

/**
 * 幂等闸门端口。用于分片重算防重复上报。
 * 实现放在基础设施层（Redis + Lua），应用层只依赖此接口。
 */
public interface IdempotencyGuard {

    /**
     * 尝试占位。返回 true 表示首次占用成功，false 表示已被占用。
     * 占位带 TTL，避免异常退出后永久锁死。
     */
    boolean tryAcquire(String key, Duration ttl);
}