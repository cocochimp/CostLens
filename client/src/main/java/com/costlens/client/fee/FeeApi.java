package com.costlens.client.fee;

import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.Platform;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 平台费用计提对外契约。
 */
public final class FeeApi {

    private FeeApi() {
    }

    public record ChargeReq(
            Platform platform,
            String category,
            long baseAmountMinor,
            Currency currency,
            int pieceCount,
            long volumeCubicCm,
            long weightGrams,
            LocalDateTime chargeTime) {
    }

    public record ChargeItemRes(
            CostType costType,
            String ruleType,
            String ruleKey,
            int ruleVersionSeq,
            long amountMinor,
            Map<String, Object> trace) {
    }

    public record ChargeRes(
            List<ChargeItemRes> items,
            long totalMinor,
            Currency currency) {
    }
}