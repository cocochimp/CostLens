package com.costlens.domain.freight.service.strategy;

import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.common.exception.ZoneUnresolvableException;
import com.costlens.common.util.Maths;
import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.freight.service.SurchargeCalculator;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Weight;
import com.costlens.model.Market;

/**
 * 美国：计费重 = max(实重, 体积重)，向上进位后 × Zone 阶梯单价。连续线性结构。
 */
public class DimWeightZoneStrategy implements FreightRateStrategy {

    private final SurchargeCalculator surchargeCalculator;

    public DimWeightZoneStrategy(SurchargeCalculator surchargeCalculator) {
        this.surchargeCalculator = surchargeCalculator == null ? SurchargeCalculator.NONE : surchargeCalculator;
    }

    @Override
    public Market market() {
        return Market.US;
    }

    @Override
    public FreightQuote quote(ShipmentContext ctx, FreightRateSnapshot snap) {
        // 体积重：立方厘米 × 1000 ÷ 抛比，整数运算
        long cubicCm = ctx.dimensions().cubicCm();
        long dimGrams = Maths.ceilDiv(cubicCm * 1000L, snap.dimDivisor());

        // 计费重 = max(实重, 体积重)，再向上进位到步长整数倍
        long rawGrams = Math.max(ctx.actualWeight().grams(), dimGrams);
        long billedGrams = Weight.ofGrams(rawGrams).roundUpTo(snap.roundStepGrams());

        int zone = snap.zoneOf(ctx.destZip())
                .orElseThrow(() -> new ZoneUnresolvableException(ctx.destZip()));

        Money unitPricePerKg = snap.usUnitPricePerKg(zone, ctx.serviceLevel(), billedGrams)
                .orElseThrow(() -> new RuleNotFoundException(
                        "未找到美国分区单价: rateKey=" + snap.rateKey() + ", zone=" + zone
                                + ", serviceLevel=" + ctx.serviceLevel() + ", billedGrams=" + billedGrams));

        Money base = unitPricePerKg.multiply(billedGrams).divide(1000L);
        Money surcharge = surchargeCalculator.surcharge(ctx, base);

        return FreightQuote.builder(snap.currency())
                .billedWeightGram(billedGrams)
                .dimWeightGram(dimGrams)
                .zone(zone)
                .baseFreight(base)
                .surcharge(surcharge)
                .trace("dimDivisor", snap.dimDivisor())
                .trace("rawGrams", rawGrams)
                .trace("billedGrams", billedGrams)
                .trace("zone", zone)
                .trace("unitPricePerKg", unitPricePerKg.toDecimal().toPlainString())
                .build();
    }
}