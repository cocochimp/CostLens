package com.costlens.model;

/**
 * 费用计提算法类型。四类算法的数学结构不同，必须分别实现。
 */
public enum ChargeRuleType {

    /** 金额 × 费率 */
    PERCENTAGE,
    /** max(金额 × 费率, 最低收费)，因最低收费而成为分段函数 */
    PERCENTAGE_FLOOR,
    /** 按金额区间分段，各段不同费率（如服装超 $250 部分仅 5%） */
    TIERED_BY_AMOUNT,
    /** 按件数 / 体积 / 重量计价 */
    PER_UNIT_BY_MEASURE
}