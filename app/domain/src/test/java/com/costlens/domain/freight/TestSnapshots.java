package com.costlens.domain.freight;

import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.rule.model.JpSizeRate;
import com.costlens.domain.rule.model.SeaFirstAddRate;
import com.costlens.domain.rule.model.UsZoneRate;
import com.costlens.domain.rule.model.ZoneRange;
import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.Currency;
import com.costlens.model.Market;

import java.util.List;

/**
 * 测试用费率快照。数值为拟真值，结构对齐真实报表。
 */
public final class TestSnapshots {

    private TestSnapshots() {
    }

    /** 美国：抛比 5000，步长 500g，Zone 8 三档单价 */
    public static FreightRateSnapshot us() {
        return new FreightRateSnapshot("UPS_GROUND_US", 1, Market.US, Currency.USD, 5000, 500,
                List.of(
                        new UsZoneRate(8, "GROUND", 0, 1_000, Money.ofMinor(800, Currency.USD)),
                        new UsZoneRate(8, "GROUND", 1_000, 2_000, Money.ofMinor(1_000, Currency.USD)),
                        new UsZoneRate(8, "GROUND", 2_000, 5_000, Money.ofMinor(1_200, Currency.USD)),
                        new UsZoneRate(3, "GROUND", 0, 1_000, Money.ofMinor(400, Currency.USD)),
                        new UsZoneRate(3, "GROUND", 1_000, 2_000, Money.ofMinor(500, Currency.USD)),
                        new UsZoneRate(3, "GROUND", 2_000, 5_000, Money.ofMinor(600, Currency.USD))),
                List.of(new ZoneRange("90000", "96199", 8),
                        new ZoneRange("00500", "00599", 3)),
                List.of(),
                List.of());
    }

    /** 日本：KANTO 地帯，按 Size 档定价 */
    public static FreightRateSnapshot jp() {
        return new FreightRateSnapshot("YAMATO_JP", 1, Market.JP, Currency.JPY, 5000, 100,
                List.of(),
                List.of(),
                List.of(
                        new JpSizeRate("KANTO", JpSizeBand.S60, Money.ofMinor(800, Currency.JPY)),
                        new JpSizeRate("KANTO", JpSizeBand.S80, Money.ofMinor(1_200, Currency.JPY)),
                        new JpSizeRate("KANTO", JpSizeBand.S100, Money.ofMinor(1_600, Currency.JPY)),
                        new JpSizeRate("KANTO", JpSizeBand.S120, Money.ofMinor(2_100, Currency.JPY))),
                List.of());
    }

    /** 东南亚：马来西亚首重 0.5kg / ¥28，续重 0.1kg / ¥3.2 */
    public static FreightRateSnapshot sea() {
        return new FreightRateSnapshot("SHOPEE_SLS_SEA", 1, Market.SEA, Currency.CNY, 5000, 100,
                List.of(),
                List.of(),
                List.of(),
                List.of(new SeaFirstAddRate("MY",
                        500L, Money.ofMinor(2_800, Currency.CNY),
                        100L, Money.ofMinor(320, Currency.CNY)),
                        new SeaFirstAddRate("TH",
                                500L, Money.ofMinor(3_200, Currency.CNY),
                                100L, Money.ofMinor(380, Currency.CNY))));
    }
}