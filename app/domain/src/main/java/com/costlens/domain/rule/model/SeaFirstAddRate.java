package com.costlens.domain.rule.model;

import com.costlens.common.util.Maths;
import com.costlens.domain.shared.model.Money;

/**
 * 东南亚首重续重价。
 * 费用 = 首重价 + ceil((实重 − 首重) / 续重单位) × 续重价。
 */
public record SeaFirstAddRate(
        String country,
        long firstWeightGram,
        Money firstPrice,
        long continuationUnitGram,
        Money continuationPrice) {

    public SeaFirstAddRate {
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("country is blank");
        }
        if (firstWeightGram <= 0 || continuationUnitGram <= 0) {
            throw new IllegalArgumentException("weight units must be positive");
        }
        if (firstPrice == null || continuationPrice == null) {
            throw new IllegalArgumentException("price is null");
        }
    }

    /** 超出首重部分折算的续重个数（整数运算，禁止浮点） */
    public long continuationUnits(long actualGrams) {
        if (actualGrams <= firstWeightGram) {
            return 0L;
        }
        return Maths.ceilDiv(actualGrams - firstWeightGram, continuationUnitGram);
    }

    public Money total(long actualGrams) {
        return firstPrice.plus(continuationPrice.multiply(continuationUnits(actualGrams)));
    }
}