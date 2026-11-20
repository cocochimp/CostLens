package com.costlens.infrastructure.cost.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.costlens.domain.cost.model.RecalcBatch;
import com.costlens.domain.cost.repository.RecalcBatchRepository;
import com.costlens.infrastructure.cost.mysql.RecalcBatchMapper;
import com.costlens.infrastructure.cost.mysql.po.RecalcBatchPO;
import com.costlens.model.RecalcBatchStatus;
import com.costlens.model.RecalcMode;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class RecalcBatchRepositoryImpl implements RecalcBatchRepository {

    private final RecalcBatchMapper mapper;

    public RecalcBatchRepositoryImpl(RecalcBatchMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<RecalcBatch> findById(String batchId) {
        RecalcBatchPO po = mapper.selectOne(Wrappers.<RecalcBatchPO>lambdaQuery()
                .eq(RecalcBatchPO::getBatchId, batchId));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(RecalcBatch.restore(po.getBatchId(),
                RecalcMode.valueOf(po.getMode()),
                po.getPeriodFrom(), po.getPeriodTo(),
                po.getShardTotal(),
                po.getShardFinished() == null ? 0 : po.getShardFinished(),
                po.getShardFailed() == null ? 0 : po.getShardFailed(),
                RecalcBatchStatus.valueOf(po.getStatus()),
                po.getStartedAt(), po.getFinishedAt()));
    }

    @Override
    public void save(RecalcBatch batch) {
        RecalcBatchPO existing = mapper.selectOne(Wrappers.<RecalcBatchPO>lambdaQuery()
                .eq(RecalcBatchPO::getBatchId, batch.batchId()));
        if (existing == null) {
            RecalcBatchPO po = new RecalcBatchPO();
            po.setBatchId(batch.batchId());
            po.setMode(batch.mode().name());
            po.setPeriodFrom(batch.periodFrom());
            po.setPeriodTo(batch.periodTo());
            po.setShardTotal(batch.shardTotal());
            po.setShardFinished(batch.shardFinished());
            po.setShardFailed(batch.shardFailed());
            po.setStatus(batch.status().name());
            po.setStartedAt(batch.startedAt());
            po.setFinishedAt(batch.finishedAt());
            mapper.insert(po);
        } else {
            existing.setShardFinished(batch.shardFinished());
            existing.setShardFailed(batch.shardFailed());
            existing.setStatus(batch.status().name());
            existing.setStartedAt(batch.startedAt());
            existing.setFinishedAt(batch.finishedAt());
            mapper.updateById(existing);
        }
    }
}