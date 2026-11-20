package com.costlens.domain.shared.model;

/**
 * 三边尺寸值对象：整数厘米。三边和用于日本 Size 档判定，必须整数运算。
 */
public record Length3(int lengthCm, int widthCm, int heightCm) {

    public Length3 {
        if (lengthCm <= 0 || widthCm <= 0 || heightCm <= 0) {
            throw new IllegalArgumentException(
                    "dimensions must be positive: " + lengthCm + "x" + widthCm + "x" + heightCm);
        }
    }

    /** 三边和（整数厘米累加，禁止浮点） */
    public int girthCm() {
        return lengthCm + widthCm + heightCm;
    }

    /** 体积（立方厘米），用于体积重换算 */
    public long cubicCm() {
        return (long) lengthCm * widthCm * heightCm;
    }
}