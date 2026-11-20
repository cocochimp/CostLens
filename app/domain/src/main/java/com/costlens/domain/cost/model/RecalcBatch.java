package com.costlens.domain.cost.model;

import com.costlens.model.RecalcBatchStatus;
import com.costlens.model.RecalcMode;

import java.time.LocalDateTime;

/**
 * 重算批次聚合根（写模式）。
 * 状态机：CREATED → RUNNING → FINISHED / PARTIAL / FAILED。
 * 分片结果累加后自动收敛批次状态，不需外部驱动。
 */
public class RecalcBatch {

    private final String batchId;
    private final RecalcMode mode;
    private final LocalDateTime periodFrom;
    private final LocalDateTime periodTo;
    private final int shardTotal;
    private int shardFinished;
    private int shardFailed;
    private RecalcBatchStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public RecalcBatch(String batchId, RecalcMode mode, LocalDateTime periodFrom,
                       LocalDateTime periodTo, int shardTotal) {
        if (batchId == null || batchId.isBlank()) {
            throw new IllegalArgumentException("batchId is blank");
        }
        if (mode == null || periodFrom == null || periodTo == null) {
            throw new IllegalArgumentException("mode and period are required");
        }
        if (shardTotal <= 0) {
            throw new IllegalArgumentException("shardTotal must be positive");
        }
        if (!periodTo.isAfter(periodFrom)) {
            throw new IllegalArgumentException("periodTo must be after periodFrom");
        }
        this.batchId = batchId;
        this.mode = mode;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.shardTotal = shardTotal;
        this.status = RecalcBatchStatus.CREATED;
    }

    /**
     * 从持久化状态恢复。仅用于仓储装配，业务代码不应直接调用。
     */
    public static RecalcBatch restore(String batchId, RecalcMode mode,
                                      LocalDateTime periodFrom, LocalDateTime periodTo,
                                      int shardTotal, int shardFinished, int shardFailed,
                                      RecalcBatchStatus status,
                                      LocalDateTime startedAt, LocalDateTime finishedAt) {
        RecalcBatch batch = new RecalcBatch(batchId, mode, periodFrom, periodTo, shardTotal);
        batch.shardFinished = shardFinished;
        batch.shardFailed = shardFailed;
        batch.status = status;
        batch.startedAt = startedAt;
        batch.finishedAt = finishedAt;
        return batch;
    }

    public void start() {
        if (status != RecalcBatchStatus.CREATED) {
            throw new IllegalStateException("只有 CREATED 可启动，当前=" + status);
        }
        this.status = RecalcBatchStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markShardFinished() {
        requireRunning();
        shardFinished++;
        settleIfDone();
    }

    public void markShardFailed() {
        requireRunning();
        shardFailed++;
        settleIfDone();
    }

    private void settleIfDone() {
        if (shardFinished + shardFailed < shardTotal) {
            return;
        }
        if (shardFailed == 0) {
            status = RecalcBatchStatus.FINISHED;
        } else if (shardFinished == 0) {
            status = RecalcBatchStatus.FAILED;
        } else {
            status = RecalcBatchStatus.PARTIAL;
        }
        finishedAt = LocalDateTime.now();
    }

    private void requireRunning() {
        if (status != RecalcBatchStatus.RUNNING) {
            throw new IllegalStateException("批次未处于 RUNNING，当前=" + status);
        }
    }

    public boolean isDone() {
        return status == RecalcBatchStatus.FINISHED
                || status == RecalcBatchStatus.FAILED
                || status == RecalcBatchStatus.PARTIAL;
    }

    public int remainingShards() {
        return shardTotal - shardFinished - shardFailed;
    }

    public String batchId() {
        return batchId;
    }

    public RecalcMode mode() {
        return mode;
    }

    public LocalDateTime periodFrom() {
        return periodFrom;
    }

    public LocalDateTime periodTo() {
        return periodTo;
    }

    public int shardTotal() {
        return shardTotal;
    }

    public int shardFinished() {
        return shardFinished;
    }

    public int shardFailed() {
        return shardFailed;
    }

    public RecalcBatchStatus status() {
        return status;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public LocalDateTime finishedAt() {
        return finishedAt;
    }
}