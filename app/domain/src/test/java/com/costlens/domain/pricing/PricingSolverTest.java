package com.costlens.domain.pricing;

import com.costlens.domain.fee.service.rule.ChargeRule;
import com.costlens.domain.fee.service.rule.PercentageRule;
import com.costlens.domain.fee.service.rule.PercentageWithFloorRule;
import com.costlens.domain.fee.service.rule.TieredByAmountRule;
import com.costlens.domain.pricing.model.PricingInput;
import com.costlens.domain.pricing.model.PricingSolution;
import com.costlens.domain.pricing.service.PricingSolver;
import com.costlens.domain.rule.model.AmountTier;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.Currency;
import com.costlens.model.InfeasibleReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingSolverTest {

    private final PricingSolver solver = new PricingSolver();

    private PricingInput input(Money fixedCost, String margin, Money cap, List<ChargeRule> rules) {
        return new PricingInput(Currency.USD, fixedCost, Rate.ofPercent(margin), cap, rules, 0, 0L, 0L);
    }

    @Test
    @DisplayName("纯比例：成本 $10、佣金 15%、目标毛利 25% → 售价 $16.67")
    void purePercentage() {
        PricingInput in = input(Money.ofMinor(1_000, Currency.USD), "25", null,
                List.of(new PercentageRule(Rate.ofPercent("15"), null)));

        PricingSolution solution = solver.solve(in);

        assertThat(solution.feasible()).isTrue();
        // 10 / (1 − 0.15 − 0.25) = 16.666… → 向上取整 16.67
        assertThat(solution.solvedPrice().minorUnits()).isEqualTo(1_667L);
    }

    @Test
    @DisplayName("最低收费使方程退化：低价区间佣金恒为 $0.30")
    void floorMakesEquationPiecewise() {
        PricingInput in = input(Money.ofMinor(50, Currency.USD), "30", null,
                List.of(new PercentageWithFloorRule(Rate.ofPercent("15"),
                        Money.ofMinor(30, Currency.USD), null)));

        PricingSolution solution = solver.solve(in);

        assertThat(solution.feasible()).isTrue();
        // 低价区间 charges(x) = 0.30 常数 → x = (0.50 + 0.30) / 0.70 = 1.1428…→ 1.15
        assertThat(solution.solvedPrice().minorUnits()).isEqualTo(115L);
        // 校验解确实落在 floor 主导区间（< $2.00）
        assertThat(solution.solvedPrice().minorUnits()).isLessThan(200L);
    }

    @Test
    @DisplayName("金额阶梯：解落在第一段时按段内费率线性求解")
    void tieredWithinFirstSlab() {
        var tiers = List.of(
                new AmountTier(Money.ofMinor(0, Currency.USD), Money.ofMinor(25_000, Currency.USD),
                        Rate.ofPercent("17")),
                new AmountTier(Money.ofMinor(25_000, Currency.USD), null, Rate.ofPercent("5")));

        PricingInput in = input(Money.ofMinor(15_000, Currency.USD), "10", null,
                List.of(new TieredByAmountRule(tiers, null)));

        PricingSolution solution = solver.solve(in);

        assertThat(solution.feasible()).isTrue();
        // 15000 / (1 − 0.17 − 0.10) = 20547.9…→ 20548（最小单位：$205.48），落在第一段内
        assertThat(solution.solvedPrice().minorUnits()).isEqualTo(20_548L);
    }

    @Test
    @DisplayName("金额阶梯：解跨段后按第二段费率求解")
    void tieredAcrossSlabs() {
        var tiers = List.of(
                new AmountTier(Money.ofMinor(0, Currency.USD), Money.ofMinor(25_000, Currency.USD),
                        Rate.ofPercent("17")),
                new AmountTier(Money.ofMinor(25_000, Currency.USD), null, Rate.ofPercent("5")));

        // 固定成本调到需要售价超过 $250
        PricingInput in = input(Money.ofMinor(24_000, Currency.USD), "10", null,
                List.of(new TieredByAmountRule(tiers, null)));

        PricingSolution solution = solver.solve(in);

        assertThat(solution.feasible()).isTrue();
        assertThat(solution.solvedPrice().minorUnits()).isGreaterThanOrEqualTo(25_000L);
    }

    @Test
    @DisplayName("核价上限低于所需售价：输出缺口金额而不是硬凑")
    void capExceededOutputsGap() {
        PricingInput in = input(Money.ofMinor(1_000, Currency.USD), "25",
                Money.ofMinor(1_200, Currency.USD),
                List.of(new PercentageRule(Rate.ofPercent("15"), null)));

        PricingSolution solution = solver.solve(in);

        assertThat(solution.feasible()).isFalse();
        assertThat(solution.reason()).isEqualTo(InfeasibleReason.CAP_EXCEEDED);
        assertThat(solution.solvedPrice().minorUnits()).isEqualTo(1_667L);
        assertThat(solution.gap().minorUnits()).isEqualTo(467L);
        assertThat(solution.message()).contains("缺口");
    }

    @Test
    @DisplayName("目标毛利率高于 1−费率：方程无解，明确报告不可达")
    void unreachableWhenRatesTooHigh() {
        PricingInput in = input(Money.ofMinor(1_000, Currency.USD), "90", null,
                List.of(new PercentageRule(Rate.ofPercent("15"), null)));

        PricingSolution solution = solver.solve(in);

        assertThat(solution.feasible()).isFalse();
        assertThat(solution.reason()).isEqualTo(InfeasibleReason.MARGIN_UNREACHABLE);
    }

    @Test
    @DisplayName("佣金与支付费叠加：两笔比例都进方程")
    void multiplePercentageRules() {
        PricingInput in = input(Money.ofMinor(1_000, Currency.USD), "20", null,
                List.of(new PercentageRule(Rate.ofPercent("15"), null),
                        new PercentageRule(Rate.ofPercent("3"), null)));

        PricingSolution solution = solver.solve(in);

        assertThat(solution.feasible()).isTrue();
        // 1000 / (1 − 0.15 − 0.03 − 0.20) = 1612.9…→ 1613
        assertThat(solution.solvedPrice().minorUnits()).isEqualTo(1_613L);
    }

    @Test
    @DisplayName("大量随机参数：要么收敛，要么明确不可行，不返回错误价格")
    void randomParametersNeverReturnWrongPrice() {
        java.util.Random random = new java.util.Random(42);
        int feasibleCount = 0;
        int infeasibleCount = 0;

        for (int i = 0; i < 1_000; i++) {
            long fixed = random.nextInt(20_000) + 100L;
            int marginPercent = random.nextInt(60) + 5;
            int commissionPercent = random.nextInt(20) + 1;
            boolean withFloor = random.nextBoolean();

            ChargeRule rule = withFloor
                    ? new PercentageWithFloorRule(Rate.ofPercent(String.valueOf(commissionPercent)),
                    Money.ofMinor(30, Currency.USD), null)
                    : new PercentageRule(Rate.ofPercent(String.valueOf(commissionPercent)), null);

            PricingInput in = input(Money.ofMinor(fixed, Currency.USD),
                    String.valueOf(marginPercent), null, List.of(rule));

            PricingSolution solution = solver.solve(in);
            if (solution.feasible()) {
                feasibleCount++;
                // 可行解必须真的满足目标毛利
                Money price = solution.solvedPrice();
                Money charge = rule.charge(
                        com.costlens.domain.fee.model.ChargeContext.ofAmount(price));
                Money profit = price.minus(in.fixedCost()).minus(charge);
                java.math.BigDecimal actual = profit.toDecimal()
                        .divide(price.toDecimal(), 8, java.math.RoundingMode.HALF_UP);
                assertThat(actual).isGreaterThanOrEqualTo(Rate.ofPercent(
                        String.valueOf(marginPercent)).value());
            } else {
                infeasibleCount++;
                assertThat(solution.reason()).isNotEqualTo(InfeasibleReason.NONE);
            }
        }

        assertThat(feasibleCount + infeasibleCount).isEqualTo(1_000);
        assertThat(feasibleCount).isGreaterThan(500);
    }
}