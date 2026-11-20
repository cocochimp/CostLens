package com.costlens.domain.pricing.service;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.fee.service.rule.ChargeRule;
import com.costlens.domain.fee.service.rule.PerUnitByMeasureRule;
import com.costlens.domain.fee.service.rule.PercentageRule;
import com.costlens.domain.fee.service.rule.PercentageWithFloorRule;
import com.costlens.domain.fee.service.rule.TieredByAmountRule;
import com.costlens.domain.pricing.model.PricingInput;
import com.costlens.domain.pricing.model.PricingSolution;
import com.costlens.domain.rule.model.AmountTier;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.InfeasibleReason;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 定价求解器（纯计算模式）。
 * 佣金与支付费是售价的分段函数，因此定价是分段方程而非除法：
 * 先按所有分段点把价格轴切开，逐段线性化求解，再回代校验。
 */
public class PricingSolver {

    private static final BigDecimal MAX_PRICE = BigDecimal.valueOf(Long.MAX_VALUE / 4);

    public PricingSolution solve(PricingInput in) {
        List<String> trace = new ArrayList<>();
        List<Long> bounds = collectBounds(in);
        trace.add("分段边界(最小单位)=" + bounds);

        for (int i = 0; i < bounds.size() - 1; i++) {
            long lo = bounds.get(i);
            long hi = bounds.get(i + 1);
            if (hi <= lo) {
                continue;
            }

            LinearForm form = linearize(in, Money.ofMinor(lo, in.currency()));
            Long solved = form.solveMinorUnits(in.fixedCost().minorUnits(),
                    in.targetMarginRate().value());
            if (solved == null) {
                trace.add("区间[" + lo + "," + hi + ") 分母非正，跳过");
                continue;
            }
            if (solved < lo || solved >= hi) {
                trace.add("区间[" + lo + "," + hi + ") 解=" + solved + " 落在区间外，换段");
                continue;
            }

            Money price = Money.ofMinor(solved, in.currency());
            if (!meetsTarget(price, in)) {
                trace.add("区间[" + lo + "," + hi + ") 解=" + solved + " 回代未达目标毛利，换段");
                continue;
            }

            trace.add("区间[" + lo + "," + hi + ") 命中解=" + solved);
            return capCheck(price, in, trace);
        }

        return PricingSolution.marginUnreachable(InfeasibleReason.MARGIN_UNREACHABLE,
                "所有价格分段均无解，目标毛利不可达", trace);
    }

    /** 收集全部分段边界：0、各规则的分段点、价格轴上界。核价上限不参与切段，只做最终约束 */
    private List<Long> collectBounds(PricingInput in) {
        Set<Long> points = new TreeSet<>();
        points.add(0L);
        for (ChargeRule rule : in.chargeRules()) {
            for (Money bp : rule.breakpoints(in.currency())) {
                if (bp.minorUnits() > 0) {
                    points.add(bp.minorUnits());
                }
            }
        }
        points.add(MAX_PRICE.longValueExact());
        return new ArrayList<>(points);
    }

    /** 在给定区间内把总费用线性化为 a·x + b。区间由分段点切出，段内各规则分支已确定 */
    private LinearForm linearize(PricingInput in, Money lo) {
        BigDecimal a = BigDecimal.ZERO;
        BigDecimal b = BigDecimal.ZERO;

        for (ChargeRule rule : in.chargeRules()) {
            if (rule instanceof PercentageRule percentage) {
                a = a.add(percentage.rate().value());
            } else if (rule instanceof PercentageWithFloorRule floorRule) {
                boolean floorDominates = lo.multiply(floorRule.rate()).compareTo(floorRule.floor()) < 0;
                if (floorDominates) {
                    b = b.add(BigDecimal.valueOf(floorRule.floor().minorUnits()));
                } else {
                    a = a.add(floorRule.rate().value());
                }
            } else if (rule instanceof TieredByAmountRule tiered) {
                TierLinear linear = tierLinearize(tiered.tiers(), lo, in);
                a = a.add(linear.marginalRate().value());
                BigDecimal offset = BigDecimal.valueOf(linear.accumulatedMinorUnits())
                        .subtract(BigDecimal.valueOf(linear.tierLowerMinorUnits())
                                .multiply(linear.marginalRate().value()));
                b = b.add(offset);
            } else if (rule instanceof PerUnitByMeasureRule) {
                // 与售价无关，表现为常数项
                Money fixed = rule.charge(ChargeContext.of(in.fixedCost(), in.pieceCount(),
                        in.volumeCubicCm(), in.weightGrams()));
                b = b.add(BigDecimal.valueOf(fixed.minorUnits()));
            } else {
                Money fixed = rule.charge(ChargeContext.ofAmount(in.fixedCost()));
                b = b.add(BigDecimal.valueOf(fixed.minorUnits()));
            }
        }
        return new LinearForm(a, b);
    }

    /** 阶梯分段在 lo 处的线性表示：charges(x) = accumulated + (x − tierLower) × marginalRate */
    private TierLinear tierLinearize(List<AmountTier> tiers, Money lo, PricingInput in) {
        long accumulated = 0L;
        for (AmountTier tier : tiers) {
            boolean coversLo = lo.compareTo(tier.lowerInclusive()) >= 0
                    && (tier.upperExclusive() == null || lo.compareTo(tier.upperExclusive()) < 0);
            if (coversLo) {
                return new TierLinear(tier.rate(), accumulated, tier.lowerInclusive().minorUnits());
            }
            if (tier.upperExclusive() != null) {
                accumulated += tier.upperExclusive().minus(tier.lowerInclusive())
                        .multiply(tier.rate()).minorUnits();
            }
        }
        // lo 超过全部阶梯且最后一段无上限：用最后一段
        AmountTier last = tiers.get(tiers.size() - 1);
        return new TierLinear(last.rate(), accumulated, last.lowerInclusive().minorUnits());
    }

    /** 回代校验：按实际规则重算一遍，确认毛利率达标 */
    private boolean meetsTarget(Money price, PricingInput in) {
        Money charges = sumCharges(price, in);
        Money profit = price.minus(in.fixedCost()).minus(charges);
        if (price.isZero()) {
            return false;
        }
        BigDecimal actualMargin = profit.toDecimal()
                .divide(price.toDecimal(), 8, java.math.RoundingMode.HALF_UP);
        return actualMargin.compareTo(in.targetMarginRate().value()) >= 0;
    }

    private Money sumCharges(Money price, PricingInput in) {
        ChargeContext ctx = ChargeContext.of(price, in.pieceCount(), in.volumeCubicCm(), in.weightGrams());
        Money sum = Money.zero(in.currency());
        for (ChargeRule rule : in.chargeRules()) {
            sum = sum.plus(rule.charge(ctx));
        }
        return sum;
    }

    private PricingSolution capCheck(Money price, PricingInput in, List<String> trace) {
        if (in.priceCap() == null || price.compareTo(in.priceCap()) <= 0) {
            return PricingSolution.feasible(price, trace);
        }
        Money gap = price.minus(in.priceCap());
        return PricingSolution.infeasibleByCap(price, in.priceCap(), gap, trace);
    }

    /** 阶梯线性化结果 */
    private record TierLinear(com.costlens.domain.shared.model.Rate marginalRate,
                              long accumulatedMinorUnits,
                              long tierLowerMinorUnits) {
    }

    /** 供外部查看分段点，便于 UI 展示 */
    public List<Long> boundsOf(PricingInput in) {
        return new ArrayList<>(new LinkedHashSet<>(collectBounds(in)));
    }
}