package com.costlens.domain.fee.service.rule;

import com.costlens.domain.shared.model.Money;

import java.util.List;

/**
 * 费用规则公共部分：封顶处理。
 */
public abstract class AbstractChargeRule implements ChargeRule {

    protected final Money cap;

    protected AbstractChargeRule(Money cap) {
        this.cap = cap;
    }

    /** 封顶：min(原始值, cap)。cap 为 null 表示不封顶 */
    protected Money applyCap(Money raw) {
        return cap == null ? raw : raw.min(cap);
    }

    @Override
    public List<Money> breakpoints(com.costlens.model.Currency currency) {
        if (cap == null) {
            return List.of();
        }
        // 封顶生效点也是一个分段边界
        return List.of(cap);
    }
}