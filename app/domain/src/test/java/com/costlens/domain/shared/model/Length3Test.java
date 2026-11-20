package com.costlens.domain.shared.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Length3Test {

    @Test
    @DisplayName("三边和是整数厘米累加")
    void girthIsIntegerSum() {
        assertThat(new Length3(26, 27, 27).girthCm()).isEqualTo(80);
        assertThat(new Length3(26, 27, 28).girthCm()).isEqualTo(81);
    }

    @Test
    @DisplayName("体积按立方厘米计算")
    void cubicCm() {
        assertThat(new Length3(30, 20, 15).cubicCm()).isEqualTo(9000L);
    }
}