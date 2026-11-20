package com.costlens.application.cost.assembler;

import com.costlens.client.cost.CostApi;
import com.costlens.domain.cost.model.OrderCostSheet;
import com.costlens.domain.cost.model.RecalcBatch;
import org.springframework.stereotype.Component;

/**
 * 成本单 / 重算批次对外契约转换。
 */
@Component
public class CostAssembler {

    public CostApi.SheetRes toSheetRes(OrderCostSheet sheet) {
        return new CostApi.SheetRes(
                sheet.sheetId(),
                sheet.orderNo(),
                sheet.status().name(),
                sheet.revenue().minorUnits(),
                sheet.totalCost().minorUnits(),
                sheet.netProfit().minorUnits(),
                sheet.netMargin().value().movePointRight(2).stripTrailingZeros().toPlainString(),
                sheet.revenue().currency(),
                sheet.items().stream()
                        .map(i -> new CostApi.SheetItemRes(i.costType(), i.ruleType(),
                                i.ruleKey(), i.ruleVersionSeq(), i.amount().minorUnits()))
                        .toList());
    }

    public CostApi.RecalcRes toRecalcRes(RecalcBatch batch) {
        return new CostApi.RecalcRes(batch.batchId(), batch.status().name(),
                batch.shardTotal(), batch.shardFinished(), batch.shardFailed(),
                batch.remainingShards());
    }
}