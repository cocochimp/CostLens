package com.costlens.common.exception;

public class OversizeException extends CostLensException {

    public OversizeException(int girthCm) {
        super(ErrorCodes.OVERSIZE, "三边和超出最大档位: girthCm=" + girthCm);
    }
}