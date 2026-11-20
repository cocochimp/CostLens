package com.costlens.domain.fee.service.rule;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.ChargeRuleType;
import com.costlens.model.MeasureKind;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 按件数 / 体积 / 重量计价。与售价无关，因此在定价求解中表现为常数项。
 */
public class PerUnitByMeasureRule extends AbstractChargeRule {

    private final MeasureKind measureKind;
    private final Money unitPrice;

    public PerUnitByMeasureRule(MeasureKind measureKind, Money unitPrice, Money cap) {
        super(cap);
        if (measureKind == null || unitPrice == null) {
            throw new IllegalArgumentException("measureKind and unitPrice are required");
        }
        this.measureKind = measureKind;
        this.unitPrice = unitPrice;
    }

    @Override
    public ChargeRuleType type() {
        return ChargeRuleType.PER_UNIT_BY_MEASURE;
    }

    @Override
    public Money charge(ChargeContext ctx) {
        BigDecimal measure = switch (measureKind) {
            case PIECE -> BigDecimal.valueOf(ctx.pieceCount());
            case VOLUME -> BigDecimal.valueOf(ctx.volumeCubicCm())
                    .divide(BigDecimal.valueOf(1_000_000L), 6, RoundingMode.HALF_UP);
            case WEIGHT -> BigDecimal.valueOf(ctx.weightGrams())
                    .divide(BigDecimal.valueOf(1_000L), 6, RoundingMode.HALF_UP);
        };
        long minor = measure.multiply(BigDecimal.valueOf(unitPrice.minorUnits()))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return applyCap(new Money(minor, unitPrice.currency()));
    }
}