package com.costlens.domain.freight.model;

import com.costlens.domain.shared.model.Length3;
import com.costlens.domain.shared.model.Weight;
import com.costlens.model.Carrier;
import com.costlens.model.Currency;
import com.costlens.model.Market;

/**
 * 运费试算上下文。三市场共用，各自只读自己需要的字段。
 */
public record ShipmentContext(
        Market market,
        Carrier carrier,
        String serviceLevel,
        String destZip,
        String area,
        String country,
        Weight actualWeight,
        Length3 dimensions,
        Currency currency) {

    public ShipmentContext {
        if (market == null || actualWeight == null || dimensions == null || currency == null) {
            throw new IllegalArgumentException("market / actualWeight / dimensions / currency are required");
        }
    }

    public static Builder builder(Market market, Currency currency) {
        return new Builder(market, currency);
    }

    public static final class Builder {
        private final Market market;
        private final Currency currency;
        private Carrier carrier;
        private String serviceLevel = "GROUND";
        private String destZip;
        private String area;
        private String country;
        private Weight actualWeight;
        private Length3 dimensions;

        private Builder(Market market, Currency currency) {
            this.market = market;
            this.currency = currency;
        }

        public Builder carrier(Carrier carrier) {
            this.carrier = carrier;
            return this;
        }

        public Builder serviceLevel(String serviceLevel) {
            this.serviceLevel = serviceLevel;
            return this;
        }

        public Builder destZip(String destZip) {
            this.destZip = destZip;
            return this;
        }

        public Builder area(String area) {
            this.area = area;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder actualWeight(Weight actualWeight) {
            this.actualWeight = actualWeight;
            return this;
        }

        public Builder dimensions(Length3 dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public ShipmentContext build() {
            return new ShipmentContext(market, carrier, serviceLevel, destZip, area, country,
                    actualWeight, dimensions, currency);
        }
    }
}