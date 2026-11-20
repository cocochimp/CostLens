package com.costlens.common.exception;

public class CurrencyMismatchException extends CostLensException {

    public CurrencyMismatchException(String actual, String expected) {
        super(ErrorCodes.CURRENCY_MISMATCH,
                "币种不一致，禁止隐式换算: actual=" + actual + ", expected=" + expected);
    }
}