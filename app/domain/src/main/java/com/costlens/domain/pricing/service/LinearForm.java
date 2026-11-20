package com.costlens.domain.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 区间内的线性式：费用(x) = a·x + b。
 * 分段函数的每个区间内都是线性的，因此可用解析法直接求解。
 */
public record LinearForm(BigDecimal a, BigDecimal b) {

    public LinearForm {
        if (a == null || b == null) {
            throw new IllegalArgumentException("a and b are required");
        }
    }

    /**
     * 解 x(1 − target) − charges(x) = fixedCost，即 x = (fixedCost + b) / (1 − a − target)。
     * 分母非正时返回 null，表示该区间无解。
     * 结果向上取整，保证达到而非略低于目标毛利。
     */
    public Long solveMinorUnits(long fixedCostMinorUnits, BigDecimal targetMargin) {
        BigDecimal denominator = BigDecimal.ONE.subtract(a).subtract(targetMargin);
        if (denominator.signum() <= 0) {
            return null;
        }
        BigDecimal numerator = BigDecimal.valueOf(fixedCostMinorUnits).add(b);
        if (numerator.signum() < 0) {
            return 0L;
        }
        return numerator.divide(denominator, 0, RoundingMode.CEILING).longValueExact();
    }
}