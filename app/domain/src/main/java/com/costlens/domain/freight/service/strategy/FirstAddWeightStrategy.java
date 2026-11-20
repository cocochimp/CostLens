package com.costlens.domain.freight.service.strategy;

import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.freight.service.SurchargeCalculator;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.rule.model.SeaFirstAddRate;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.Market;

/**
 * 东南亚：首重价 + ceil((实重 − 首重) / 续重单位) × 续重价。分段 + 向上取整结构。
 */
public class FirstAddWeightStrategy implements FreightRateStrategy {

    private final SurchargeCalculator surchargeCalculator;

    public FirstAddWeightStrategy(SurchargeCalculator surchargeCalculator) {
        this.surchargeCalculator = surchargeCalculator == null ? SurchargeCalculator.NONE : surchargeCalculator;
    }

    @Override
    public Market market() {
        return Market.SEA;
    }

    @Override
    public FreightQuote quote(ShipmentContext ctx, FreightRateSnapshot snap) {
        long actualGrams = ctx.actualWeight().grams();

        SeaFirstAddRate rate = snap.seaRate(ctx.country())
                .orElseThrow(() -> new RuleNotFoundException(
                        "未找到东南亚首重续重价: rateKey=" + snap.rateKey() + ", country=" + ctx.country()));

        long units = rate.continuationUnits(actualGrams);
        Money total = rate.total(actualGrams);
        Money surcharge = surchargeCalculator.surcharge(ctx, total);

        return FreightQuote.builder(snap.currency())
                .billedWeightGram(actualGrams)
                .continuationUnits(units)
                .baseFreight(total)
                .surcharge(surcharge)
                .trace("firstWeightGram", rate.firstWeightGram())
                .trace("continuationUnitGram", rate.continuationUnitGram())
                .trace("continuationUnits", units)
                .trace("actualGrams", actualGrams)
                .build();
    }
}