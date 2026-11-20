package com.costlens.application;

import com.costlens.domain.rule.model.AmountTier;
import com.costlens.domain.rule.model.FeeRule;
import com.costlens.domain.rule.model.FeeRuleParams;
import com.costlens.domain.rule.model.FreightRateCard;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.rule.model.JpSizeRate;
import com.costlens.domain.rule.model.SeaFirstAddRate;
import com.costlens.domain.rule.model.UsZoneRate;
import com.costlens.domain.rule.model.ZoneRange;
import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.Carrier;
import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.Market;
import com.costlens.model.Platform;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 端到端测试种子数据：结构与数值对齐真实报表口径。
 */
public final class Fixtures {

    public static final LocalDateTime T0 = LocalDateTime.of(2023, 1, 1, 0, 0);
    public static final LocalDateTime T1 = LocalDateTime.of(2023, 3, 1, 0, 0);
    public static final LocalDateTime BIZ = LocalDateTime.of(2023, 3, 15, 10, 0);
    public static final LocalDateTime RECORD = LocalDateTime.of(2023, 1, 1, 0, 0);

    private Fixtures() {
    }

    public static FreightRateSnapshot usSnapshot() {
        return new FreightRateSnapshot("UPS_GROUND", 1, Market.US, Currency.USD, 5000, 500,
                List.of(
                        new UsZoneRate(8, "GROUND", 0, 1_000, Money.ofMinor(800, Currency.USD)),
                        new UsZoneRate(8, "GROUND", 1_000, 2_000, Money.ofMinor(1_000, Currency.USD)),
                        new UsZoneRate(8, "GROUND", 2_000, 5_000, Money.ofMinor(1_200, Currency.USD))),
                List.of(new ZoneRange("90000", "96199", 8),
                        new ZoneRange("00500", "00599", 3)),
                List.of(), List.of());
    }

    public static FreightRateSnapshot jpSnapshot() {
        return new FreightRateSnapshot("YAMATO_TAKKYUUBIN", 1, Market.JP, Currency.JPY, 5000, 100,
                List.of(), List.of(),
                List.of(new JpSizeRate("KANTO", JpSizeBand.S60, Money.ofMinor(800, Currency.JPY)),
                        new JpSizeRate("KANTO", JpSizeBand.S80, Money.ofMinor(1_200, Currency.JPY)),
                        new JpSizeRate("KANTO", JpSizeBand.S100, Money.ofMinor(1_600, Currency.JPY))),
                List.of());
    }

    public static FreightRateSnapshot seaSnapshot() {
        return new FreightRateSnapshot("SHOPEE_SLS", 1, Market.SEA, Currency.CNY, 5000, 100,
                List.of(), List.of(), List.of(),
                List.of(new SeaFirstAddRate("MY", 500L, Money.ofMinor(2_800, Currency.CNY),
                        100L, Money.ofMinor(320, Currency.CNY))));
    }

    public static FreightRateCard usCard() {
        FreightRateCard card = new FreightRateCard("UPS_GROUND", Carrier.UPS_GROUND);
        card.appendVersion(usSnapshot(), T0, null, RECORD);
        return card;
    }

    public static FreightRateCard jpCard() {
        FreightRateCard card = new FreightRateCard("YAMATO_TAKKYUUBIN", Carrier.YAMATO_TAKKYUUBIN);
        card.appendVersion(jpSnapshot(), T0, null, RECORD);
        return card;
    }

    public static FreightRateCard seaCard() {
        FreightRateCard card = new FreightRateCard("SHOPEE_SLS", Carrier.SHOPEE_SLS);
        card.appendVersion(seaSnapshot(), T0, null, RECORD);
        return card;
    }

    /** Amazon 美国电子类目佣金：15% + 最低 $0.30 */
    public static FeeRule commissionWithFloor() {
        FeeRule rule = new FeeRule("AMAZON_US_ELECTRONICS_COMMISSION",
                Platform.AMAZON_US, "ELECTRONICS", CostType.COMMISSION);
        rule.appendVersion(
                FeeRuleParams.percentageWithFloor(CostType.COMMISSION, Rate.ofPercent("15"),
                        Money.ofMinor(30, Currency.USD)),
                T0, null, RECORD);
        return rule;
    }

    /** 支付手续费 3% */
    public static FeeRule paymentRule() {
        FeeRule rule = new FeeRule("AMAZON_US_ALL_PAYMENT",
                Platform.AMAZON_US, "ALL", CostType.PAYMENT);
        rule.appendVersion(FeeRuleParams.percentage(CostType.PAYMENT, Rate.ofPercent("3")),
                T0, null, RECORD);
        return rule;
    }

    /** 服装类目分段佣金：$250 以内 17%，超出 5% */
    public static FeeRule apparelTiered() {
        FeeRule rule = new FeeRule("AMAZON_US_APPAREL_COMMISSION",
                Platform.AMAZON_US, "APPAREL", CostType.COMMISSION);
        rule.appendVersion(FeeRuleParams.tieredByAmount(CostType.COMMISSION, List.of(
                        new AmountTier(Money.ofMinor(0, Currency.USD),
                                Money.ofMinor(25_000, Currency.USD), Rate.ofPercent("17")),
                        new AmountTier(Money.ofMinor(25_000, Currency.USD), null, Rate.ofPercent("5")))),
                T0, null, RECORD);
        return rule;
    }

    /** Shopee 马来佣金：两版本，v2 于 2023-07-01 生效 */
    public static FeeRule shopeeMyCommission() {
        FeeRule rule = new FeeRule("SHOPEE_MY_ALL_COMMISSION",
                Platform.SHOPEE_MY, "ALL", CostType.COMMISSION);
        rule.appendVersion(FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("5.5")),
                T0, LocalDateTime.of(2023, 7, 1, 0, 0), RECORD);
        rule.appendVersion(FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("6")),
                LocalDateTime.of(2023, 7, 1, 0, 0), null, T1);
        return rule;
    }
}