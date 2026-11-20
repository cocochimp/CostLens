package com.costlens.domain.rule.model;

import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.Currency;
import com.costlens.model.Market;

import java.util.List;
import java.util.Optional;

/**
 * 运费费率快照：某一版本的完整运价数据，不可变，供策略计算使用。
 */
public record FreightRateSnapshot(
        String rateKey,
        int versionSeq,
        Market market,
        Currency currency,
        long dimDivisor,
        long roundStepGrams,
        List<UsZoneRate> usZoneRates,
        List<ZoneRange> zoneRanges,
        List<JpSizeRate> jpSizeRates,
        List<SeaFirstAddRate> seaFirstAddRates) {

    public FreightRateSnapshot {
        if (rateKey == null || rateKey.isBlank()) {
            throw new IllegalArgumentException("rateKey is blank");
        }
        if (market == null || currency == null) {
            throw new IllegalArgumentException("market and currency are required");
        }
        if (dimDivisor <= 0 || roundStepGrams <= 0) {
            throw new IllegalArgumentException("dimDivisor and roundStepGrams must be positive");
        }
        usZoneRates = usZoneRates == null ? List.of() : List.copyOf(usZoneRates);
        zoneRanges = zoneRanges == null ? List.of() : List.copyOf(zoneRanges);
        jpSizeRates = jpSizeRates == null ? List.of() : List.copyOf(jpSizeRates);
        seaFirstAddRates = seaFirstAddRates == null ? List.of() : List.copyOf(seaFirstAddRates);
    }

    /** 美国：Zone × 服务等级 × 重量段 → 每公斤单价 */
    public Optional<Money> usUnitPricePerKg(int zone, String serviceLevel, long grams) {
        return usZoneRates.stream()
                .filter(r -> r.zone() == zone)
                .filter(r -> r.serviceLevel().equals(serviceLevel))
                .filter(r -> r.matches(grams))
                .map(UsZoneRate::unitPricePerKg)
                .findFirst();
    }

    /** 邮编前缀 → Zone。区间查找，前缀定长字符串保住前导零 */
    public Optional<Integer> zoneOf(String zip) {
        if (zip == null || zip.trim().length() < 5) {
            return Optional.empty();
        }
        String prefix = zip.trim().substring(0, 5);
        return zoneRanges.stream()
                .filter(r -> r.zipPrefixFrom().length() == 5)
                .filter(r -> r.contains(prefix))
                .findFirst()
                .map(ZoneRange::zone);
    }

    /** 日本：地帯 × Size 档 → 价格 */
    public Optional<Money> jpPrice(String area, JpSizeBand band) {
        return jpSizeRates.stream()
                .filter(r -> r.area().equals(area))
                .filter(r -> r.sizeBand() == band)
                .map(JpSizeRate::price)
                .findFirst();
    }

    /** 东南亚：国家 → 首重续重价 */
    public Optional<SeaFirstAddRate> seaRate(String country) {
        return seaFirstAddRates.stream()
                .filter(r -> r.country().equalsIgnoreCase(country))
                .findFirst();
    }
}