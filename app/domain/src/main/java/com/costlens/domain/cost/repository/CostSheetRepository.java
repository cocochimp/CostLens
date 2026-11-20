package com.costlens.domain.cost.repository;

import com.costlens.domain.cost.model.OrderCostSheet;

import java.util.Optional;

public interface CostSheetRepository {

    Optional<OrderCostSheet> findById(String sheetId);

    Optional<OrderCostSheet> findByOrderNo(String orderNo, String recalcBatchId);

    void save(OrderCostSheet sheet);
}