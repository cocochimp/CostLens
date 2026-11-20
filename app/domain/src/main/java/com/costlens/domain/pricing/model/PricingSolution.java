package com.costlens.domain.pricing.model;

import com.costlens.domain.shared.model.Money;
import com.costlens.model.InfeasibleReason;

import java.util.List;

/**
 * 定价求解结果。不可行时输出缺口金额，不静默返回近似价。
 */
public record PricingSolution(
        boolean feasible,
        Money solvedPrice,
        Money priceCap,
        Money gap,
        InfeasibleReason reason,
        String message,
        List<String> trace) {

    public PricingSolution {
        trace = trace == null ? List.of() : List.copyOf(trace);
    }

    public static PricingSolution feasible(Money price, List<String> trace) {
        return new PricingSolution(true, price, null, null, InfeasibleReason.NONE, "ok", trace);
    }

    public static PricingSolution infeasibleByCap(Money required, Money cap, Money gap, List<String> trace) {
        return new PricingSolution(false, required, cap, gap, InfeasibleReason.CAP_EXCEEDED,
                "达到目标毛利需售价 " + required + "，超出核价上限 " + cap + "，缺口 " + gap, trace);
    }

    public static PricingSolution marginUnreachable(InfeasibleReason reason, String message, List<String> trace) {
        return new PricingSolution(false, null, null, null, reason, message, trace);
    }
}