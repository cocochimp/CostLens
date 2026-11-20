package com.costlens.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MathsTest {

    @Test
    @DisplayName("向上取整除法基本语义")
    void ceilDiv() {
        assertThat(Maths.ceilDiv(200, 100)).isEqualTo(2);
        assertThat(Maths.ceilDiv(201, 100)).isEqualTo(3);
        assertThat(Maths.ceilDiv(0, 100)).isZero();
        assertThat(Maths.ceilDiv(99, 100)).isEqualTo(1);
    }

    @Test
    @DisplayName("负数场景必须先分支，直接拒绝而不是返回错误结果")
    void ceilDivRejectsNegative() {
        // Java 整数除法向零截断：(-200 + 99) / 100 = -1，与 ceil(-2) 无关
        assertThatThrownBy(() -> Maths.ceilDiv(-200, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("除数为 0 拒绝")
    void ceilDivRejectsZeroDivisor() {
        assertThatThrownBy(() -> Maths.ceilDiv(100, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}