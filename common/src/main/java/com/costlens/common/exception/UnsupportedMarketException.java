package com.costlens.common.exception;

public class UnsupportedMarketException extends CostLensException {

    public UnsupportedMarketException(String market) {
        super(ErrorCodes.UNSUPPORTED_MARKET, "不支持的计费市场: " + market);
    }
}