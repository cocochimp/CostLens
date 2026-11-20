package com.costlens.domain.cost.repository;

import com.costlens.domain.cost.model.RecalcBatch;

import java.util.Optional;

public interface RecalcBatchRepository {

    Optional<RecalcBatch> findById(String batchId);

    void save(RecalcBatch batch);
}