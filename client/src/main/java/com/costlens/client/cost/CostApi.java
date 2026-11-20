package com.costlens.client.cost;

import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.Market;
import com.costlens.model.Platform;
import com.costlens.model.RecalcMode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 成本单 / 重算批次对外契约。
 */
public final class CostApi {

    private CostApi() {
    }

    public record FreightSpec(
            Market market,
            String serviceLevel,
            String destZip,
            String area,
            String country,
            long actualWeightGram,
            int lengthCm,
            int widthCm,
            int heightCm) {
    }

    public record FixedCostSpec(
            CostType costType,
            long amountMinor,
            String ruleKey) {
    }

    public record SheetReq(
            String sheetId,
            String orderNo,
            Platform platform,
            String category,
            long revenueMinor,
            Currency currency,
            LocalDateTime bizTime,
            FreightSpec freight,
            List<FixedCostSpec> fixedCosts) {
    }

    public record SheetItemRes(
            CostType costType,
            String ruleType,
            String ruleKey,
            int ruleVersionSeq,
            long amountMinor) {
    }

    public record SheetRes(
            String sheetId,
            String orderNo,
            String status,
            long revenueMinor,
            long totalCostMinor,
            long netProfitMinor,
            String netMarginPercent,
            Currency currency,
            List<SheetItemRes> items) {
    }

    public record RecalcReq(
            RecalcMode mode,
            LocalDateTime periodFrom,
            LocalDateTime periodTo,
            int shardTotal) {
    }

    public record RecalcRes(
            String batchId,
            String status,
            int shardTotal,
            int shardFinished,
            int shardFailed,
            int remainingShards) {
    }
}