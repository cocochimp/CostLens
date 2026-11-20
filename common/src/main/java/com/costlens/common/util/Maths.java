package com.costlens.common.util;

/**
 * 整数运算工具。计价链路严禁浮点，向上取整必须走这里。
 */
public final class Maths {

    private Maths() {
    }

    /**
     * 向上取整除法。仅接受非负被除数与正除数。
     * Java 整数除法向零截断，负数会让 ceil 语义失效，因此这里直接拒绝。
     */
    public static long ceilDiv(long dividend, long divisor) {
        if (dividend < 0) {
            throw new IllegalArgumentException("ceilDiv requires dividend >= 0, got " + dividend);
        }
        if (divisor <= 0) {
            throw new IllegalArgumentException("ceilDiv requires divisor > 0, got " + divisor);
        }
        return (dividend + divisor - 1) / divisor;
    }
}