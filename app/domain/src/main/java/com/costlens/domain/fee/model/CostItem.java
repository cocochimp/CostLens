package com.costlens.domain.fee.model;

import com.costlens.domain.shared.model.Money;
import com.costlens.model.CostType;

import java.util.Map;

/**
 * 费用项明细。每一分钱都能追到规则版本与计算过程。
 * ruleType 用字符串而不是 ChargeRuleType：运费项不属于费用计提算法，
 * 强行塞进枚举会让语义失真。
 */
public record CostItem(
        CostType costType,
        String ruleType,
        String ruleKey,
        int ruleVersionSeq,
        Money amount,
        Map<String, Object> trace) {

    public CostItem {
        if (costType == null || ruleType == null || amount == null) {
            throw new IllegalArgumentException("costType / ruleType / amount are required");
        }
        trace = trace == null ? Map.of() : Map.copyOf(trace);
    }
}