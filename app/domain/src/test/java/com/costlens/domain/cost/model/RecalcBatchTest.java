package com.costlens.domain.cost.model;

import com.costlens.model.RecalcBatchStatus;
import com.costlens.model.RecalcMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecalcBatchTest {

    private RecalcBatch batch(int shards) {
        return new RecalcBatch("B1", RecalcMode.AS_OF_ORIGINAL,
                LocalDateTime.of(2023, 1, 1, 0, 0), LocalDateTime.of(2023, 1, 31, 0, 0), shards);
    }

    @Test
    @DisplayName("全部分片成功 → FINISHED")
    void allShardsSucceed() {
        RecalcBatch batch = batch(3);
        batch.start();
        batch.markShardFinished();
        batch.markShardFinished();
        assertThat(batch.isDone()).isFalse();
        batch.markShardFinished();

        assertThat(batch.status()).isEqualTo(RecalcBatchStatus.FINISHED);
        assertThat(batch.remainingShards()).isZero();
    }

    @Test
    @DisplayName("部分分片失败 → PARTIAL，可单独重跑")
    void partialFailure() {
        RecalcBatch batch = batch(3);
        batch.start();
        batch.markShardFinished();
        batch.markShardFailed();
        batch.markShardFinished();

        assertThat(batch.status()).isEqualTo(RecalcBatchStatus.PARTIAL);
        assertThat(batch.shardFailed()).isEqualTo(1);
    }

    @Test
    @DisplayName("全部分片失败 → FAILED")
    void allFailed() {
        RecalcBatch batch = batch(2);
        batch.start();
        batch.markShardFailed();
        batch.markShardFailed();

        assertThat(batch.status()).isEqualTo(RecalcBatchStatus.FAILED);
    }

    @Test
    @DisplayName("未启动就报分片结果直接拒绝")
    void cannotReportBeforeStart() {
        RecalcBatch batch = batch(2);
        assertThatThrownBy(batch::markShardFinished).isInstanceOf(IllegalStateException.class);
    }
}