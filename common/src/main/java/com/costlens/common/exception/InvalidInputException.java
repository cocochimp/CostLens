package com.costlens.common.exception;

public class InvalidInputException extends CostLensException {

    public InvalidInputException(String message) {
        super(ErrorCodes.INVALID_INPUT, message);
    }
}