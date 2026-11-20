package com.costlens.domain.freight.service.strategy;

import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.freight.service.SurchargeCalculator;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.Market;

/**
 * 日本：三边和 → Size 档 → 地帯价。阶跃查表结构，重量只做上限校验，不参与计价。
 */
public class GirthSizeBandStrategy implements FreightRateStrategy {

    private final SurchargeCalculator surchargeCalculator;

    public GirthSizeBandStrategy(SurchargeCalculator surchargeCalculator) {
        this.surchargeCalculator = surchargeCalculator == null ? SurchargeCalculator.NONE : surchargeCalculator;
    }

    @Override
    public Market market() {
        return Market.JP;
    }

    @Override
    public FreightQuote quote(ShipmentContext ctx, FreightRateSnapshot snap) {
        // 三边和：整数厘米累加，含上界落档
        int girthCm = ctx.dimensions().girthCm();
        JpSizeBand bandByGirth = JpSizeBand.ofGirth(girthCm);

        // 超重只升级档位，不按重量计价
        final JpSizeBand band = ctx.actualWeight().grams() > bandByGirth.weightCapGrams()
                ? bandByGirth.upgradeFor(ctx.actualWeight())
                : bandByGirth;

        Money price = snap.jpPrice(ctx.area(), band)
                .orElseThrow(() -> new RuleNotFoundException(
                        "未找到日本 Size 价格: rateKey=" + snap.rateKey()
                                + ", area=" + ctx.area() + ", band=" + band));

        Money surcharge = surchargeCalculator.surcharge(ctx, price);

        return FreightQuote.builder(snap.currency())
                .billedWeightGram(ctx.actualWeight().grams())
                .girthCm(girthCm)
                .sizeBand(band)
                .baseFreight(price)
                .surcharge(surcharge)
                .trace("girthCm", girthCm)
                .trace("bandByGirth", bandByGirth.name())
                .trace("bandFinal", band.name())
                .trace("weightCapGrams", band.weightCapGrams())
                .trace("actualGrams", ctx.actualWeight().grams())
                .build();
    }
}