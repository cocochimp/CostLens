package com.costlens.model;

/**
 * 币种及其最小单位精度。金额一律以最小单位整数表示。
 */
public enum Currency {

    USD(2),
    JPY(0),
    EUR(2),
    GBP(2),
    MYR(2),
    THB(2),
    IDR(0),
    CNY(2);

    private final int minorUnit;

    Currency(int minorUnit) {
        this.minorUnit = minorUnit;
    }

    public int minorUnit() {
        return minorUnit;
    }

    public static Currency of(String code) {
        if (code == null) {
            throw new IllegalArgumentException("currency code is null");
        }
        return Currency.valueOf(code.trim().toUpperCase());
    }
}