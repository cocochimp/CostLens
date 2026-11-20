package com.costlens.domain.fee.service.rule;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.ChargeRuleType;

/**
 * 比例计提：金额 × 费率。
 */
public class PercentageRule extends AbstractChargeRule {

    private final Rate rate;

    public PercentageRule(Rate rate, Money cap) {
        super(cap);
        if (rate == null) {
            throw new IllegalArgumentException("rate is null");
        }
        this.rate = rate;
    }

    @Override
    public ChargeRuleType type() {
        return ChargeRuleType.PERCENTAGE;
    }

    public Rate rate() {
        return rate;
    }

    @Override
    public Money charge(ChargeContext ctx) {
        return applyCap(ctx.baseAmount().multiply(rate));
    }
}