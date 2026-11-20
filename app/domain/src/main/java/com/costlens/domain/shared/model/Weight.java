package com.costlens.domain.shared.model;

import com.costlens.common.util.Maths;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 重量值对象：整数克。禁止用 double 累加，否则会多算续重。
 */
public record Weight(long grams) implements Comparable<Weight> {

    public static final Weight ZERO = new Weight(0L);

    public Weight {
        if (grams < 0) {
            throw new IllegalArgumentException("weight must be >= 0, got " + grams);
        }
    }

    public static Weight ofGrams(long grams) {
        return new Weight(grams);
    }

    /** 解析形如 "1.23" 的千克数 */
    public static Weight ofKilograms(String kg) {
        BigDecimal g = new BigDecimal(kg).movePointRight(3).setScale(0, RoundingMode.HALF_UP);
        return new Weight(g.longValueExact());
    }

    public Weight plus(Weight other) {
        return new Weight(Math.addExact(grams, other.grams));
    }

    /** 向上取整到 stepGrams 的整数倍 */
    public long roundUpTo(long stepGrams) {
        return Maths.ceilDiv(grams, stepGrams) * stepGrams;
    }

    public boolean isGreaterThan(Weight other) {
        return grams > other.grams;
    }

    @Override
    public int compareTo(Weight o) {
        return Long.compare(grams, o.grams);
    }
}