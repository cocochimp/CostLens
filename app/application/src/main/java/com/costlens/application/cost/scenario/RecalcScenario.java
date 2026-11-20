package com.costlens.application.cost.scenario;

import com.costlens.application.cost.assembler.CostAssembler;
import com.costlens.application.cost.port.IdempotencyGuard;
import com.costlens.client.cost.CostApi;
import com.costlens.common.exception.CostLensException;
import com.costlens.common.exception.ErrorCodes;
import com.costlens.domain.cost.model.RecalcBatch;
import com.costlens.domain.cost.repository.RecalcBatchRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 重算批次场景：创建 → 启动 → 分片回报 → 收敛状态。
 * 分片回报走幂等闸门，同一个 (batchId, shardId) 只会计入一次。
 */
@Service
public class RecalcScenario {

    /** 分片幂等键存活 6 小时，覆盖一次完整重算窗口 */
    private static final Duration SHARD_IDEMPOTENCY_TTL = Duration.ofHours(6);

    private final RecalcBatchRepository repository;
    private final IdempotencyGuard idempotencyGuard;
    private final CostAssembler assembler;

    public RecalcScenario(RecalcBatchRepository repository,
                          IdempotencyGuard idempotencyGuard,
                          CostAssembler assembler) {
        this.repository = repository;
        this.idempotencyGuard = idempotencyGuard;
        this.assembler = assembler;
    }

    public CostApi.RecalcRes create(CostApi.RecalcReq req) {
        String batchId = "RB" + System.currentTimeMillis();
        RecalcBatch batch = new RecalcBatch(batchId, req.mode(),
                req.periodFrom(), req.periodTo(), req.shardTotal());
        batch.start();
        repository.save(batch);
        return assembler.toRecalcRes(batch);
    }

    public CostApi.RecalcRes reportShard(String batchId, int shardId, boolean success) {
        String idempotencyKey = "recalc:shard:" + batchId + ":" + shardId;
        if (!idempotencyGuard.tryAcquire(idempotencyKey, SHARD_IDEMPOTENCY_TTL)) {
            throw new IllegalStateException("分片重复上报已拦截: batchId=" + batchId + ", shardId=" + shardId);
        }

        RecalcBatch batch = repository.findById(batchId)
                .orElseThrow(() -> new CostLensException(ErrorCodes.BATCH_ALREADY_EXISTS,
                        "批次不存在: " + batchId));
        if (success) {
            batch.markShardFinished();
        } else {
            batch.markShardFailed();
        }
        repository.save(batch);
        return assembler.toRecalcRes(batch);
    }

    public CostApi.RecalcRes query(String batchId) {
        RecalcBatch batch = repository.findById(batchId)
                .orElseThrow(() -> new CostLensException(ErrorCodes.BATCH_ALREADY_EXISTS,
                        "批次不存在: " + batchId));
        return assembler.toRecalcRes(batch);
    }
}