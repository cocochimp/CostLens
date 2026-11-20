package com.costlens.model;

/**
 * 尾程承运商。每个承运商对应对应一个市场与一套计价规则。
 */
public enum Carrier {

    UPS_GROUND(Market.US),
    YAMATO_TAKKYUUBIN(Market.JP),
    SHOPEE_SLS(Market.SEA);

    private final Market market;

    Carrier(Market market) {
        this.market = market;
    }

    public Market market() {
        return market;
    }
}