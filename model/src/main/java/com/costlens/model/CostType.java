package com.costlens.model;

/**
 * 成本项类型。成本单由这些项汇总而成。
 */
public enum CostType {

    /** 运费（头程或尾程），由运费策略族计算 */
    FREIGHT,
    /** 平台佣金 */
    COMMISSION,
    /** 支付手续费 */
    PAYMENT,
    /** 仓储费 */
    STORAGE,
    /** 广告费 */
    ADVERTISING
}