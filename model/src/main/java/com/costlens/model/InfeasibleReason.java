package com.costlens.model;

/**
 * 定价求解不可行的原因。宁可输出不可行，也不静默返回近似价。
 */
public enum InfeasibleReason {

    /** 可行 */
    NONE,
    /** 所需售价超出平台核价上限 */
    CAP_EXCEEDED,
    /** 所有价格分段均无解，目标毛利不可达 */
    MARGIN_UNREACHABLE,
    /** 费率之和导致分母非正，方程无意义 */
    RATE_TOO_HIGH
}