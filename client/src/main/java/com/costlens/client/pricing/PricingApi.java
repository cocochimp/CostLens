package com.costlens.client.pricing;

import com.costlens.model.Currency;
import com.costlens.model.Platform;

import java.util.List;

/**
 * 定价求解对外契约。
 */
public final class PricingApi {

    private PricingApi() {
    }

    public record SolveReq(
            Platform platform,
            String category,
            long fixedCostMinor,
            Currency currency,
            String targetMarginPercent,
            Long priceCapMinor,
            int pieceCount,
            long volumeCubicCm,
            long weightGrams) {
    }

    public record SolveRes(
            boolean feasible,
            Long solvedPriceMinor,
            Long priceCapMinor,
            Long gapMinor,
            String reason,
            String message,
            List<String> trace) {
    }
}