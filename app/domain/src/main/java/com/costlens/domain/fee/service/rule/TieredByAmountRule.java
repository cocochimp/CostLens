package com.costlens.domain.fee.service.rule;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.rule.model.AmountTier;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.ChargeRuleType;
import com.costlens.model.Currency;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 金额分段计提。例：服装类目 $250 以内 17%，超过部分 5%。
 */
public class TieredByAmountRule extends AbstractChargeRule {

    private final List<AmountTier> tiers;

    public TieredByAmountRule(List<AmountTier> tiers, Money cap) {
        super(cap);
        if (tiers == null || tiers.isEmpty()) {
            throw new IllegalArgumentException("tiers is empty");
        }
        List<AmountTier> sorted = new ArrayList<>(tiers);
        sorted.sort(Comparator.comparing(AmountTier::lowerInclusive));
        this.tiers = List.copyOf(sorted);
    }

    @Override
    public ChargeRuleType type() {
        return ChargeRuleType.TIERED_BY_AMOUNT;
    }

    @Override
    public Money charge(ChargeContext ctx) {
        Money amount = ctx.baseAmount();
        Money sum = Money.zero(amount.currency());

        for (AmountTier tier : tiers) {
            if (amount.compareTo(tier.lowerInclusive()) <= 0) {
                break;
            }
            Money slabTop = tier.upperExclusive() == null
                    ? amount
                    : amount.min(tier.upperExclusive());
            Money slab = slabTop.minus(tier.lowerInclusive());
            if (slab.signum() <= 0) {
                continue;
            }
            sum = sum.plus(slab.multiply(tier.rate()));
            if (tier.upperExclusive() == null) {
                break;
            }
        }
        return applyCap(sum);
    }

    @Override
    public List<Money> breakpoints(Currency currency) {
        List<Money> points = new ArrayList<>(super.breakpoints(currency));
        for (AmountTier tier : tiers) {
            if (tier.upperExclusive() != null) {
                points.add(tier.upperExclusive());
            }
        }
        return points;
    }

    public List<AmountTier> tiers() {
        return tiers;
    }
}