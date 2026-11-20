package com.costlens.domain.rule.model;

/**
 * 运费费率卡的一个版本：时间坐标 + 运价快照。只追加，不修改。
 */
public record FreightRateVersion(RuleVersionRef ref, FreightRateSnapshot snapshot) {

    public FreightRateVersion {
        if (ref == null || snapshot == null) {
            throw new IllegalArgumentException("ref and snapshot are required");
        }
        if (!ref.ruleKey().equals(snapshot.rateKey())) {
            throw new IllegalArgumentException(
                    "rateKey mismatch: ref=" + ref.ruleKey() + ", snapshot=" + snapshot.rateKey());
        }
    }
}