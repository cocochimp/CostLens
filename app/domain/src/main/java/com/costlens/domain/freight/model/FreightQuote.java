package com.costlens.domain.freight.model;

import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.Currency;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运费试算结果。trace 保留全部计价因子，供钻取与复算校验。
 */
public record FreightQuote(
        Currency currency,
        long billedWeightGram,
        Long dimWeightGram,
        Integer zone,
        Integer girthCm,
        JpSizeBand sizeBand,
        Long continuationUnits,
        Money baseFreight,
        Money surcharge,
        Money total,
        Map<String, Object> trace) {

    public FreightQuote {
        if (currency == null || baseFreight == null || surcharge == null || total == null) {
            throw new IllegalArgumentException("currency / baseFreight / surcharge / total are required");
        }
        trace = trace == null ? Map.of() : Map.copyOf(trace);
    }

    public static Builder builder(Currency currency) {
        return new Builder(currency);
    }

    public static final class Builder {
        private final Currency currency;
        private long billedWeightGram;
        private Long dimWeightGram;
        private Integer zone;
        private Integer girthCm;
        private JpSizeBand sizeBand;
        private Long continuationUnits;
        private Money baseFreight;
        private Money surcharge;
        private final Map<String, Object> trace = new LinkedHashMap<>();

        private Builder(Currency currency) {
            this.currency = currency;
        }

        public Builder billedWeightGram(long v) {
            this.billedWeightGram = v;
            return this;
        }

        public Builder dimWeightGram(Long v) {
            this.dimWeightGram = v;
            return this;
        }

        public Builder zone(Integer v) {
            this.zone = v;
            return this;
        }

        public Builder girthCm(Integer v) {
            this.girthCm = v;
            return this;
        }

        public Builder sizeBand(JpSizeBand v) {
            this.sizeBand = v;
            return this;
        }

        public Builder continuationUnits(Long v) {
            this.continuationUnits = v;
            return this;
        }

        public Builder baseFreight(Money v) {
            this.baseFreight = v;
            return this;
        }

        public Builder surcharge(Money v) {
            this.surcharge = v;
            return this;
        }

        public Builder trace(String key, Object value) {
            this.trace.put(key, value);
            return this;
        }

        public FreightQuote build() {
            Money sf = surcharge == null ? Money.zero(currency) : surcharge;
            Money total = baseFreight.plus(sf);
            return new FreightQuote(currency, billedWeightGram, dimWeightGram, zone, girthCm,
                    sizeBand, continuationUnits, baseFreight, sf, total, trace);
        }
    }
}