package com.costlens.common.exception;

/**
 * 所有领域异常的基类，携带业务错误码。
 */
public class CostLensException extends RuntimeException {

    private final int code;

    public CostLensException(int code, String message) {
        super(message);
        this.code = code;
    }

    public CostLensException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}