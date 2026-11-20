package com.costlens.common.exception;

/**
 * 业务错误码。与设计说明书 §5.3 一一对应。
 */
public final class ErrorCodes {

    /** 费率规则不存在 */
    public static final int RULE_NOT_FOUND = 1001;
    /** 规则版本区间或记录时点冲突 */
    public static final int RULE_VERSION_CONFLICT = 1002;
    /** 邮编无法解析到分区 */
    public static final int ZONE_UNRESOLVABLE = 1003;
    /** 三边和超出最大档位 */
    public static final int OVERSIZE = 2001;
    /** 币种不一致 */
    public static final int CURRENCY_MISMATCH = 2002;
    /** 不支持的计费市场 */
    public static final int UNSUPPORTED_MARKET = 2003;
    /** 输入非法（邮编、重量、尺寸等） */
    public static final int INVALID_INPUT = 2004;
    /** 定价无解 */
    public static final int PRICING_INFEASIBLE = 3001;
    /** 核价约束冲突 */
    public static final int PRICE_CAP_CONFLICT = 3002;
    /** 重算批次已存在 */
    public static final int BATCH_ALREADY_EXISTS = 4001;

    private ErrorCodes() {
    }
}