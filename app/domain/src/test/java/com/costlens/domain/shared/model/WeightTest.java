package com.costlens.domain.shared.model;

import com.costlens.common.util.Maths;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightTest {

    @Test
    @DisplayName("多件重量累加必须整数精确，不得多算一个续重")
    void multiPieceAccumulationIsExact() {
        // 两件货：0.2kg + 0.4kg
        Weight total = Weight.ofKilograms("0.2").plus(Weight.ofKilograms("0.4"));
        assertThat(total.grams()).isEqualTo(600L);

        // 若用 double：0.2 + 0.4 = 0.6000000000000001
        //           (0.6000000000000001 - 0.5) / 0.1 = 1.0000000000000009 → ceil = 2，多收一个续重
        double floating = 0.2 + 0.4;
        assertThat(Math.ceil((floating - 0.5) / 0.1)).isEqualTo(2.0);

        // 整数链路：600g − 500g = 100g，100 ÷ 100 = 1 个续重
        long units = total.grams() <= 500 ? 0 : Maths.ceilDiv(total.grams() - 500, 100);
        assertThat(units).isEqualTo(1L);
    }

    @Test
    @DisplayName("不足首重时续重个数为 0")
    void belowFirstWeight() {
        Weight total = Weight.ofKilograms("0.3");
        long units = total.grams() <= 500 ? 0 : Maths.ceilDiv(total.grams() - 500, 100);
        assertThat(units).isZero();
    }

    @Test
    @DisplayName("计费重向上取整到 500g")
    void roundUpToHalfKilogram() {
        assertThat(Weight.ofGrams(1200).roundUpTo(500)).isEqualTo(1500L);
        assertThat(Weight.ofGrams(1500).roundUpTo(500)).isEqualTo(1500L);
        assertThat(Weight.ofGrams(1501).roundUpTo(500)).isEqualTo(2000L);
    }

    @Test
    @DisplayName("千克字符串解析不引入浮点")
    void parseKilograms() {
        assertThat(Weight.ofKilograms("0.1").grams()).isEqualTo(100L);
        assertThat(Weight.ofKilograms("1.005").grams()).isEqualTo(1005L);
    }

    @Test
    @DisplayName("负重量直接拒绝")
    void negativeRejected() {
        assertThatThrownBy(() -> Weight.ofGrams(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}