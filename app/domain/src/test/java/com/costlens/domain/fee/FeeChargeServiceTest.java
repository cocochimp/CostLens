package com.costlens.domain.fee;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.fee.model.CostItem;
import com.costlens.domain.fee.model.FeeChargeResult;
import com.costlens.domain.fee.service.FeeChargeService;
import com.costlens.domain.rule.model.AmountTier;
import com.costlens.domain.rule.model.FeeRuleParams;
import com.costlens.domain.rule.model.FeeRuleVersion;
import com.costlens.domain.rule.model.RuleVersionRef;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.MeasureKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FeeChargeServiceTest {

    private final FeeChargeService service = new FeeChargeService();
    private static final LocalDateTime T = LocalDateTime.of(2023, 1, 1, 0, 0);

    private FeeRuleVersion version(String key, FeeRuleParams params) {
        return new FeeRuleVersion(
                new RuleVersionRef(1L, key, 1, T, null, T), params);
    }

    @Test
    @DisplayName("比例计提：$100.00 × 15% = $15.00")
    void percentage() {
        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(10_000, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("COMMISSION", FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("15")))));

        assertThat(result.total().minorUnits()).isEqualTo(1_500L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).costType()).isEqualTo(CostType.COMMISSION);
    }

    @Test
    @DisplayName("最低收费：$1.00 商品实收 $0.30，实际费率 30%")
    void floorDominatesLowPrice() {
        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(100, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("COMMISSION", FeeRuleParams.percentageWithFloor(
                        CostType.COMMISSION, Rate.ofPercent("15"), Money.ofMinor(30, Currency.USD)))));

        assertThat(result.total().minorUnits()).isEqualTo(30L);
        // 名义 15% → 实际 30%
        assertThat(result.total().minorUnits() * 100 / 100).isEqualTo(30L);
    }

    @Test
    @DisplayName("最低收费：$10.00 商品按比例 $1.50，不受 floor 影响")
    void percentageWinsAboveThreshold() {
        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(1_000, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("COMMISSION", FeeRuleParams.percentageWithFloor(
                        CostType.COMMISSION, Rate.ofPercent("15"), Money.ofMinor(30, Currency.USD)))));

        assertThat(result.total().minorUnits()).isEqualTo(150L);
    }

    @Test
    @DisplayName("金额分段：$300 服装 = 前 $250×17% + 后 $50×5% = $45.00")
    void tieredByAmount() {
        var tiers = List.of(
                new AmountTier(Money.ofMinor(0, Currency.USD), Money.ofMinor(25_000, Currency.USD),
                        Rate.ofPercent("17")),
                new AmountTier(Money.ofMinor(25_000, Currency.USD), null, Rate.ofPercent("5")));

        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(30_000, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("COMMISSION_APPAREL", FeeRuleParams.tieredByAmount(CostType.COMMISSION, tiers))));

        // 4250 + 250 = 4500，等效费率 15% 而非 17%
        assertThat(result.total().minorUnits()).isEqualTo(4_500L);
    }

    @Test
    @DisplayName("按件计价：3 件 × $0.50 = $1.50")
    void perUnit() {
        ChargeContext ctx = ChargeContext.of(Money.ofMinor(5_000, Currency.USD), 3, 0L, 0L);
        FeeChargeResult result = service.charge(ctx, List.of(
                version("STORAGE", FeeRuleParams.perUnit(CostType.STORAGE, MeasureKind.PIECE,
                        Money.ofMinor(50, Currency.USD)))));

        assertThat(result.total().minorUnits()).isEqualTo(150L);
    }

    @Test
    @DisplayName("按重量计价：2.5kg × $0.40/kg = $1.00")
    void perWeight() {
        ChargeContext ctx = ChargeContext.of(Money.ofMinor(5_000, Currency.USD), 0, 0L, 2_500L);
        FeeChargeResult result = service.charge(ctx, List.of(
                version("STORAGE", FeeRuleParams.perUnit(CostType.STORAGE, MeasureKind.WEIGHT,
                        Money.ofMinor(40, Currency.USD)))));

        assertThat(result.total().minorUnits()).isEqualTo(100L);
    }

    @Test
    @DisplayName("多项费用逐项汇总，明细不合并")
    void multipleItems() {
        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(10_000, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("COMMISSION", FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("15"))),
                version("PAYMENT", FeeRuleParams.percentage(CostType.PAYMENT, Rate.ofPercent("3")))));

        assertThat(result.items()).hasSize(2);
        assertThat(result.total().minorUnits()).isEqualTo(1_800L);
        assertThat(result.amountOf(CostType.COMMISSION).minorUnits()).isEqualTo(1_500L);
        assertThat(result.amountOf(CostType.PAYMENT).minorUnits()).isEqualTo(300L);
    }

    @Test
    @DisplayName("互斥组只保留优先级最高的一条")
    void mutexGroupKeepsHighestPriority() {
        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(10_000, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("PROMO_FREE", FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("0"))
                        .withMutex("PROMO", 1)),
                version("PROMO_FEE", FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("2"))
                        .withMutex("PROMO", 5))));

        assertThat(result.items()).hasSize(1);
        assertThat(result.total().minorUnits()).isEqualTo(200L);
    }

    @Test
    @DisplayName("封顶：不超过设定上限")
    void capApplied() {
        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(100_000, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("COMMISSION", FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("15"))
                        .withCap(Money.ofMinor(5_000, Currency.USD)))));

        assertThat(result.total().minorUnits()).isEqualTo(5_000L);
    }

    @Test
    @DisplayName("明细携带规则版本号，可钻取复算")
    void traceCarriesRuleVersion() {
        ChargeContext ctx = ChargeContext.ofAmount(Money.ofMinor(10_000, Currency.USD));
        FeeChargeResult result = service.charge(ctx, List.of(
                version("COMMISSION", FeeRuleParams.percentage(CostType.COMMISSION, Rate.ofPercent("15")))));

        CostItem item = result.items().get(0);
        assertThat(item.ruleKey()).isEqualTo("COMMISSION");
        assertThat(item.ruleVersionSeq()).isEqualTo(1);
        assertThat(item.trace()).containsKey("baseAmount");
    }
}