package com.costlens.domain.fee.service.rule;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.ChargeRuleType;
import com.costlens.model.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 比例 + 最低收费：max(金额 × 费率, floor)。
 * 因最低收费，该规则在金额轴上是分段函数，临界点为 floor / rate。
 */
public class PercentageWithFloorRule extends AbstractChargeRule {

    private final Rate rate;
    private final Money floor;

    public PercentageWithFloorRule(Rate rate, Money floor, Money cap) {
        super(cap);
        if (rate == null || floor == null) {
            throw new IllegalArgumentException("rate and floor are required");
        }
        if (!rate.isPositive()) {
            throw new IllegalArgumentException("rate must be positive for floor rule");
        }
        this.rate = rate;
        this.floor = floor;
    }

    public Money floor() {
        return floor;
    }

    public Rate rate() {
        return rate;
    }

    /** 最低收费生效的临界价格：floor / rate，向上取整到最小单位 */
    public Money thresholdAmount() {
        BigDecimal threshold = BigDecimal.valueOf(floor.minorUnits())
                .divide(rate.value(), 0, RoundingMode.CEILING);
        return new Money(threshold.longValueExact(), floor.currency());
    }

    @Override
    public ChargeRuleType type() {
        return ChargeRuleType.PERCENTAGE_FLOOR;
    }

    @Override
    public Money charge(ChargeContext ctx) {
        Money proportional = ctx.baseAmount().multiply(rate);
        return applyCap(proportional.max(floor));
    }

    @Override
    public List<Money> breakpoints(Currency currency) {
        List<Money> points = new ArrayList<>(super.breakpoints(currency));
        points.add(thresholdAmount());
        return points;
    }
}