package com.costlens.domain.fee.service.rule;

import com.costlens.domain.rule.model.FeeRuleParams;

/**
 * 按规则类型创建计提算法实例。
 */
public final class ChargeRuleFactory {

    private ChargeRuleFactory() {
    }

    public static ChargeRule of(FeeRuleParams params) {
        return switch (params.ruleType()) {
            case PERCENTAGE -> new PercentageRule(params.rate(), params.cap());
            case PERCENTAGE_FLOOR -> new PercentageWithFloorRule(params.rate(), params.floor(), params.cap());
            case TIERED_BY_AMOUNT -> new TieredByAmountRule(params.tiers(), params.cap());
            case PER_UNIT_BY_MEASURE ->
                    new PerUnitByMeasureRule(params.measureKind(), params.unitPrice(), params.cap());
        };
    }
}