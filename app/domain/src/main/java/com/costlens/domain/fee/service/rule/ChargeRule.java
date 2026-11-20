package com.costlens.domain.fee.service.rule;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.ChargeRuleType;
import com.costlens.model.Currency;

import java.util.List;

/**
 * 费用计提算法。四类算法的数学结构不同，必须分别实现。
 */
public interface ChargeRule {

    ChargeRuleType type();

    Money charge(ChargeContext ctx);

    /**
     * 该规则在金额轴上的分段边界（升序）。
     * 定价求解据此把价格轴切成区间，区间内函数才是线性的。
     */
    List<Money> breakpoints(Currency currency);
}