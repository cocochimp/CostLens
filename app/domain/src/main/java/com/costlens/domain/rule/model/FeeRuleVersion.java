package com.costlens.domain.rule.model;

/**
 * 费用规则的一个版本：时间坐标 + 参数体。只追加，不修改。
 */
public record FeeRuleVersion(RuleVersionRef ref, FeeRuleParams params) {

    public FeeRuleVersion {
        if (ref == null || params == null) {
            throw new IllegalArgumentException("ref and params are required");
        }
    }
}