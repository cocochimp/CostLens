package com.costlens.domain.rule.model;

import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.ChargeRuleType;
import com.costlens.model.CostType;
import com.costlens.model.MeasureKind;

import java.util.List;

/**
 * 费用规则参数。按 ruleType 取用对应字段，其余为 null。
 */
public record FeeRuleParams(
        CostType costType,
        ChargeRuleType ruleType,
        Rate rate,
        Money floor,
        List<AmountTier> tiers,
        MeasureKind measureKind,
        Money unitPrice,
        Money cap,
        String mutexGroup,
        int priority) {

    public FeeRuleParams {
        if (costType == null || ruleType == null) {
            throw new IllegalArgumentException("costType and ruleType are required");
        }
        switch (ruleType) {
            case PERCENTAGE -> require(rate != null, "PERCENTAGE 需要 rate");
            case PERCENTAGE_FLOOR -> {
                require(rate != null, "PERCENTAGE_FLOOR 需要 rate");
                require(floor != null, "PERCENTAGE_FLOOR 需要 floor");
            }
            case TIERED_BY_AMOUNT -> require(tiers != null && !tiers.isEmpty(), "TIERED_BY_AMOUNT 需要 tiers");
            case PER_UNIT_BY_MEASURE -> {
                require(measureKind != null, "PER_UNIT_BY_MEASURE 需要 measureKind");
                require(unitPrice != null, "PER_UNIT_BY_MEASURE 需要 unitPrice");
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static FeeRuleParams percentage(CostType costType, Rate rate) {
        return new FeeRuleParams(costType, ChargeRuleType.PERCENTAGE, rate, null, null,
                null, null, null, null, 0);
    }

    public static FeeRuleParams percentageWithFloor(CostType costType, Rate rate, Money floor) {
        return new FeeRuleParams(costType, ChargeRuleType.PERCENTAGE_FLOOR, rate, floor, null,
                null, null, null, null, 0);
    }

    public static FeeRuleParams tieredByAmount(CostType costType, List<AmountTier> tiers) {
        return new FeeRuleParams(costType, ChargeRuleType.TIERED_BY_AMOUNT, null, null, tiers,
                null, null, null, null, 0);
    }

    public static FeeRuleParams perUnit(CostType costType, MeasureKind kind, Money unitPrice) {
        return new FeeRuleParams(costType, ChargeRuleType.PER_UNIT_BY_MEASURE, null, null, null,
                kind, unitPrice, null, null, 0);
    }

    public FeeRuleParams withCap(Money newCap) {
        return new FeeRuleParams(costType, ruleType, rate, floor, tiers,
                measureKind, unitPrice, newCap, mutexGroup, priority);
    }

    public FeeRuleParams withMutex(String group, int newPriority) {
        return new FeeRuleParams(costType, ruleType, rate, floor, tiers,
                measureKind, unitPrice, cap, group, newPriority);
    }
}