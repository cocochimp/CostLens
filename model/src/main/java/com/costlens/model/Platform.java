package com.costlens.model;

/**
 * 电商平台站点。费用规则按平台 + 类目匹配。
 */
public enum Platform {

    AMAZON_US(Market.US, Currency.USD),
    AMAZON_JP(Market.JP, Currency.JPY),
    SHOPEE_MY(Market.SEA, Currency.MYR),
    SHOPEE_TH(Market.SEA, Currency.THB),
    SHOPEE_ID(Market.SEA, Currency.IDR),
    TEMU_SEMI(Market.SEA, Currency.USD);

    private final Market market;
    private final Currency defaultCurrency;

    Platform(Market market, Currency defaultCurrency) {
        this.market = market;
        this.defaultCurrency = defaultCurrency;
    }

    public Market market() {
        return market;
    }

    public Currency defaultCurrency() {
        return defaultCurrency;
    }
}