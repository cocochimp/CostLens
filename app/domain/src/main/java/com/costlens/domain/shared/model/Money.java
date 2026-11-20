package com.costlens.domain.shared.model;

import com.costlens.common.exception.CurrencyMismatchException;
import com.costlens.model.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金额值对象：币种最小单位整数。禁止用 double 表示金额。
 */
public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

    public Money {
        if (currency == null) {
            throw new IllegalArgumentException("currency is null");
        }
    }

    public static Money ofMinor(long minorUnits, Currency currency) {
        return new Money(minorUnits, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(0L, currency);
    }

    /** 解析形如 "12.34" 的金额（按币种最小单位解释） */
    public static Money ofDecimal(String amount, Currency currency) {
        BigDecimal scaled = new BigDecimal(amount)
                .movePointRight(currency.minorUnit())
                .setScale(0, RoundingMode.HALF_UP);
        return new Money(scaled.longValueExact(), currency);
    }

    public Money plus(Money other) {
        assertSameCurrency(other);
        return new Money(Math.addExact(minorUnits, other.minorUnits), currency);
    }

    public Money minus(Money other) {
        assertSameCurrency(other);
        return new Money(Math.subtractExact(minorUnits, other.minorUnits), currency);
    }

    /**
     * 乘以定点费率。中间用 BigDecimal 计算，末位 HALF_UP 收敛回最小单位整数。
     */
    public Money multiply(Rate rate) {
        BigDecimal v = BigDecimal.valueOf(minorUnits)
                .multiply(rate.value())
                .setScale(0, RoundingMode.HALF_UP);
        return new Money(v.longValueExact(), currency);
    }

    public Money multiply(long times) {
        return new Money(Math.multiplyExact(minorUnits, times), currency);
    }

    public Money divide(long divisor) {
        if (divisor == 0) {
            throw new IllegalArgumentException("divisor is zero");
        }
        BigDecimal v = BigDecimal.valueOf(minorUnits)
                .divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_UP);
        return new Money(v.longValueExact(), currency);
    }

    public Money min(Money other) {
        assertSameCurrency(other);
        return minorUnits <= other.minorUnits ? this : other;
    }

    public Money max(Money other) {
        assertSameCurrency(other);
        return minorUnits >= other.minorUnits ? this : other;
    }

    public boolean isNegative() {
        return minorUnits < 0;
    }

    public boolean isZero() {
        return minorUnits == 0;
    }

    public int signum() {
        return Long.signum(minorUnits);
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(minorUnits).movePointLeft(currency.minorUnit());
    }

    private void assertSameCurrency(Money other) {
        if (other.currency != currency) {
            throw new CurrencyMismatchException(other.currency.name(), currency.name());
        }
    }

    @Override
    public int compareTo(Money o) {
        assertSameCurrency(o);
        return Long.compare(minorUnits, o.minorUnits);
    }

    @Override
    public String toString() {
        return toDecimal().toPlainString() + " " + currency.name();
    }
}