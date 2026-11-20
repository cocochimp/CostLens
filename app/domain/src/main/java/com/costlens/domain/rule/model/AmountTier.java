package com.costlens.domain.rule.model;

import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;

/**
 * 金额分段。upperExclusive 为 null 表示无上限。
 * 服装类目「$250 以上部分按 5%」即两段。
 */
public record AmountTier(Money lowerInclusive, Money upperExclusive, Rate rate) {

    public AmountTier {
        if (lowerInclusive == null || lowerInclusive.isNegative()) {
            throw new IllegalArgumentException("lowerInclusive must be >= 0");
        }
        if (upperExclusive != null && upperExclusive.compareTo(lowerInclusive) <= 0) {
            throw new IllegalArgumentException("upperExclusive must be greater than lowerInclusive");
        }
        if (rate == null) {
            throw new IllegalArgumentException("rate is null");
        }
    }

    public boolean contains(Money amount) {
        if (amount.compareTo(lowerInclusive) < 0) {
            return false;
        }
        return upperExclusive == null || amount.compareTo(upperExclusive) < 0;
    }
}