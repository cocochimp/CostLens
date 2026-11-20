package com.costlens.domain.pricing.model;

import com.costlens.domain.fee.service.rule.ChargeRule;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.Currency;

import java.util.List;

/**
 * 定价求解输入。
 * fixedCost 为与售价无关的固定成本（采购、运费、仓储、广告）；
 * chargeRules 为依赖售价的费用规则（佣金、支付手续费）。
 */
public record PricingInput(
        Currency currency,
        Money fixedCost,
        Rate targetMarginRate,
        Money priceCap,
        List<ChargeRule> chargeRules,
        int pieceCount,
        long volumeCubicCm,
        long weightGrams) {

    public PricingInput {
        if (currency == null || fixedCost == null || targetMarginRate == null) {
            throw new IllegalArgumentException("currency / fixedCost / targetMarginRate are required");
        }
        if (!fixedCost.currency().equals(currency)) {
            throw new IllegalArgumentException("fixedCost currency mismatch");
        }
        if (targetMarginRate.value().signum() < 0
                || targetMarginRate.value().compareTo(java.math.BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("targetMarginRate must be within [0, 1)");
        }
        if (priceCap != null && !priceCap.currency().equals(currency)) {
            throw new IllegalArgumentException("priceCap currency mismatch");
        }
        chargeRules = chargeRules == null ? List.of() : List.copyOf(chargeRules);
    }
}