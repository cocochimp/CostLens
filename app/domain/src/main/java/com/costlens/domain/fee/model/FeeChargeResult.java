package com.costlens.domain.fee.model;

import com.costlens.domain.shared.model.Money;
import com.costlens.model.CostType;

import java.util.List;

/**
 * 费用计提结果：逐项明细 + 汇总。明细永不合并，保证可解释性。
 */
public record FeeChargeResult(List<CostItem> items, Money total) {

    public FeeChargeResult {
        items = items == null ? List.of() : List.copyOf(items);
        if (total == null) {
            throw new IllegalArgumentException("total is null");
        }
    }

    public Money amountOf(CostType costType) {
        return items.stream()
                .filter(i -> i.costType() == costType)
                .map(CostItem::amount)
                .reduce(Money.zero(total.currency()), Money::plus);
    }
}