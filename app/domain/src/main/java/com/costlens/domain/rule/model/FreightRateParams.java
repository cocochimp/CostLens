package com.costlens.domain.rule.model;

/**
 * 运费费率卡的公共参数，落库在 params_json。
 */
public record FreightRateParams(long dimDivisor, long roundStepGrams) {

    public FreightRateParams {
        if (dimDivisor <= 0) {
            throw new IllegalArgumentException("dimDivisor must be positive, got " + dimDivisor);
        }
        if (roundStepGrams <= 0) {
            throw new IllegalArgumentException("roundStepGrams must be positive, got " + roundStepGrams);
        }
    }
}