package com.costlens.domain.rule.model;

import com.costlens.domain.shared.model.Money;

/**
 * 美国分区阶梯单价：[weightFromGram, weightToGram) 区间内每公斤单价（币种最小单位）。
 */
public record UsZoneRate(
        int zone,
        String serviceLevel,
        long weightFromGram,
        long weightToGram,
        Money unitPricePerKg) {

    public UsZoneRate {
        if (zone < 1 || zone > 8) {
            throw new IllegalArgumentException("zone must be within 1..8, got " + zone);
        }
        if (weightFromGram < 0 || weightToGram <= weightFromGram) {
            throw new IllegalArgumentException("invalid weight band: " + weightFromGram + "~" + weightToGram);
        }
        if (unitPricePerKg == null || unitPricePerKg.isNegative()) {
            throw new IllegalArgumentException("unitPricePerKg must be >= 0");
        }
    }

    public boolean matches(long grams) {
        return grams >= weightFromGram && grams < weightToGram;
    }
}