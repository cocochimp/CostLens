package com.costlens.domain.shared.model;

import com.costlens.common.exception.CurrencyMismatchException;
import com.costlens.model.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    @DisplayName("比例计提按币种最小单位 HALF_UP 收敛")
    void multiplyByRate() {
        // $1.00 × 15% = $0.15
        assertThat(Money.ofMinor(100, Currency.USD).multiply(Rate.ofPercent("15")).minorUnits())
                .isEqualTo(15L);
        // ¥1000 × 15% = ¥150（JPY 无小数）
        assertThat(Money.ofMinor(1000, Currency.JPY).multiply(Rate.ofPercent("15")).minorUnits())
                .isEqualTo(150L);
    }

    @Test
    @DisplayName("比例计提不足一分时按 HALF_UP 舍入")
    void multiplyRoundsHalfUp() {
        // 1 分 × 50% = 0.5 分 → 1 分
        assertThat(Money.ofMinor(1, Currency.USD).multiply(Rate.ofPercent("50")).minorUnits())
                .isEqualTo(1L);
        // 1 分 × 49% = 0.49 分 → 0 分
        assertThat(Money.ofMinor(1, Currency.USD).multiply(Rate.ofPercent("49")).minorUnits())
                .isEqualTo(0L);
    }

    @Test
    @DisplayName("最低收费使低价商品实际费率高于名义费率")
    void floorChargeDominates() {
        var commission = Money.ofMinor(100, Currency.USD).multiply(Rate.ofPercent("15"));
        var floor = Money.ofMinor(30, Currency.USD);
        // 名义 15% 只该收 $0.15，最低收费 $0.30 生效 → 实际费率 30%
        assertThat(commission.max(floor).minorUnits()).isEqualTo(30L);
        assertThat(commission.max(floor).minorUnits() * 100 / 100).isEqualTo(30L);
    }

    @Test
    @DisplayName("币种不一致直接拒绝，不做隐式换算")
    void currencyMismatchRejected() {
        assertThatThrownBy(() -> Money.ofMinor(100, Currency.USD).plus(Money.ofMinor(100, Currency.JPY)))
                .isInstanceOf(CurrencyMismatchException.class);
    }

    @Test
    @DisplayName("min/max 与减法保持整数精确")
    void arithmeticIsExact() {
        var a = Money.ofMinor(2500, Currency.USD);
        var b = Money.ofMinor(600, Currency.USD);
        assertThat(a.minus(b).minorUnits()).isEqualTo(1900L);
        assertThat(a.min(b).minorUnits()).isEqualTo(600L);
        assertThat(a.max(b).minorUnits()).isEqualTo(2500L);
    }

    @Test
    @DisplayName("decimal 解析按币种精度解释")
    void parseDecimal() {
        assertThat(Money.ofDecimal("12.34", Currency.USD).minorUnits()).isEqualTo(1234L);
        assertThat(Money.ofDecimal("1500", Currency.JPY).minorUnits()).isEqualTo(1500L);
    }
}