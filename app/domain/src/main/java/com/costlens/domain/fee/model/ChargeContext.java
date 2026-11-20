package com.costlens.domain.fee.model;

import com.costlens.domain.shared.model.Money;

/**
 * 费用计提上下文。baseAmount 是计费基数（通常是订单金额），其余为计量口径。
 */
public record ChargeContext(
        Money baseAmount,
        int pieceCount,
        long volumeCubicCm,
        long weightGrams) {

    public ChargeContext {
        if (baseAmount == null || baseAmount.isNegative()) {
            throw new IllegalArgumentException("baseAmount must be >= 0");
        }
        if (pieceCount < 0 || volumeCubicCm < 0 || weightGrams < 0) {
            throw new IllegalArgumentException("measures must be >= 0");
        }
    }

    public static ChargeContext ofAmount(Money amount) {
        return new ChargeContext(amount, 0, 0L, 0L);
    }

    public static ChargeContext of(Money amount, int pieces, long cubicCm, long grams) {
        return new ChargeContext(amount, pieces, cubicCm, grams);
    }
}