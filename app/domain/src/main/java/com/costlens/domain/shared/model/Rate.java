package com.costlens.domain.shared.model;

import java.math.BigDecimal;

/**
 * 定点费率，scale 固定为 8。所有比例类计算必须传这个类型，禁止裸 double。
 */
public record Rate(BigDecimal value) implements Comparable<Rate> {

    public static final int SCALE = 8;

    public Rate {
        if (value == null) {
            throw new IllegalArgumentException("rate value is null");
        }
        value = value.setScale(SCALE, java.math.RoundingMode.HALF_UP);
    }

    public static Rate ofPercent(String percent) {
        return new Rate(new BigDecimal(percent).movePointLeft(2));
    }

    public static Rate ofPercent(BigDecimal percent) {
        return new Rate(percent.movePointLeft(2));
    }

    public static Rate ofRatio(BigDecimal ratio) {
        return new Rate(ratio);
    }

    public static Rate zero() {
        return new Rate(BigDecimal.ZERO);
    }

    public Rate plus(Rate other) {
        return new Rate(value.add(other.value));
    }

    public Rate minus(Rate other) {
        return new Rate(value.subtract(other.value));
    }

    public boolean isPositive() {
        return value.signum() > 0;
    }

    @Override
    public int compareTo(Rate o) {
        return value.compareTo(o.value);
    }
}