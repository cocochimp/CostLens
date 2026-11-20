package com.costlens.infrastructure.cost.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.costlens.domain.cost.model.OrderCostSheet;
import com.costlens.domain.cost.repository.CostSheetRepository;
import com.costlens.domain.fee.model.CostItem;
import com.costlens.domain.shared.model.Money;
import com.costlens.infrastructure.cost.mysql.CostItemDetailMapper;
import com.costlens.infrastructure.cost.mysql.OrderCostSheetMapper;
import com.costlens.infrastructure.cost.mysql.po.CostItemDetailPO;
import com.costlens.infrastructure.cost.mysql.po.OrderCostSheetPO;
import com.costlens.infrastructure.shared.JsonCodec;
import com.costlens.model.ChargeRuleType;
import com.costlens.model.CostSheetStatus;
import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.Platform;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 成本单仓储实现。成本单与其明细同库同事务。
 */
@Repository
public class CostSheetRepositoryImpl implements CostSheetRepository {

    private static final TypeReference<Map<String, Object>> TRACE_TYPE = new TypeReference<>() {
    };

    private final OrderCostSheetMapper sheetMapper;
    private final CostItemDetailMapper detailMapper;
    private final JsonCodec codec;

    public CostSheetRepositoryImpl(OrderCostSheetMapper sheetMapper,
                                   CostItemDetailMapper detailMapper,
                                   JsonCodec codec) {
        this.sheetMapper = sheetMapper;
        this.detailMapper = detailMapper;
        this.codec = codec;
    }

    @Override
    public Optional<OrderCostSheet> findById(String sheetId) {
        OrderCostSheetPO po = sheetMapper.selectOne(Wrappers.<OrderCostSheetPO>lambdaQuery()
                .eq(OrderCostSheetPO::getSheetId, sheetId));
        if (po == null) {
            return Optional.empty();
        }
        List<CostItem> items = detailMapper.selectList(Wrappers.<CostItemDetailPO>lambdaQuery()
                        .eq(CostItemDetailPO::getSheetId, sheetId))
                .stream()
                .map(this::toItem)
                .toList();
        return Optional.of(new OrderCostSheet(po.getSheetId(), po.getOrderNo(),
                Platform.valueOf(po.getPlatform()),
                Money.ofMinor(po.getRevenueMinor(), Currency.of(po.getCurrency())),
                items, CostSheetStatus.valueOf(po.getStatus()), po.getRecalcBatchId()));
    }

    @Override
    public Optional<OrderCostSheet> findByOrderNo(String orderNo, String recalcBatchId) {
        var query = Wrappers.<OrderCostSheetPO>lambdaQuery().eq(OrderCostSheetPO::getOrderNo, orderNo);
        if (recalcBatchId == null) {
            query.isNull(OrderCostSheetPO::getRecalcBatchId);
        } else {
            query.eq(OrderCostSheetPO::getRecalcBatchId, recalcBatchId);
        }
        OrderCostSheetPO po = sheetMapper.selectOne(query);
        return po == null ? Optional.empty() : findById(po.getSheetId());
    }

    @Override
    public void save(OrderCostSheet sheet) {
        OrderCostSheetPO existing = sheetMapper.selectOne(Wrappers.<OrderCostSheetPO>lambdaQuery()
                .eq(OrderCostSheetPO::getSheetId, sheet.sheetId()));

        OrderCostSheetPO po = new OrderCostSheetPO();
        po.setSheetId(sheet.sheetId());
        po.setOrderNo(sheet.orderNo());
        po.setPlatform(sheet.platform().name());
        po.setRevenueMinor(sheet.revenue().minorUnits());
        po.setTotalCostMinor(sheet.totalCost().minorUnits());
        po.setCurrency(sheet.revenue().currency().name());
        po.setStatus(sheet.status().name());
        po.setRecalcBatchId(sheet.recalcBatchId());

        if (existing == null) {
            po.setCreatedAt(LocalDateTime.now());
            sheetMapper.insert(po);
            for (CostItem item : sheet.items()) {
                detailMapper.insert(toPO(sheet.sheetId(), item));
            }
        } else {
            // 状态流转只更新状态字段，明细不重写
            existing.setStatus(sheet.status().name());
            existing.setRecalcBatchId(sheet.recalcBatchId());
            sheetMapper.updateById(existing);
        }
    }

    private CostItem toItem(CostItemDetailPO po) {
        Map<String, Object> trace = codec.read(po.getTraceJson(), TRACE_TYPE);
        return new CostItem(CostType.valueOf(po.getCostType()),
                po.getRuleType(),
                po.getRuleKey(),
                po.getRuleVersionSeq() == null ? 0 : po.getRuleVersionSeq(),
                Money.ofMinor(po.getComputedMinor(), Currency.of(po.getCurrency())),
                trace);
    }

    private CostItemDetailPO toPO(String sheetId, CostItem item) {
        CostItemDetailPO po = new CostItemDetailPO();
        po.setSheetId(sheetId);
        po.setCostType(item.costType().name());
        po.setRuleType(item.ruleType());
        po.setRuleKey(item.ruleKey());
        po.setRuleVersionSeq(item.ruleVersionSeq());
        po.setComputedMinor(item.amount().minorUnits());
        po.setCurrency(item.amount().currency().name());
        po.setTraceJson(codec.write(item.trace()));
        return po;
    }
}