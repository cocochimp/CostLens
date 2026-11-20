package com.costlens.domain.rule.model;

import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Money;

/**
 * 日本 Size 档价格：地帯 × Size → 价格。重量不参与计价，仅由 Size 档隐含上限。
 */
public record JpSizeRate(String area, JpSizeBand sizeBand, Money price) {

    public JpSizeRate {
        if (area == null || area.isBlank()) {
            throw new IllegalArgumentException("area is blank");
        }
        if (sizeBand == null) {
            throw new IllegalArgumentException("sizeBand is null");
        }
        if (price == null || price.isNegative()) {
            throw new IllegalArgumentException("price must be >= 0");
        }
    }
}