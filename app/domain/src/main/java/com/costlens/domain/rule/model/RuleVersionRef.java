package com.costlens.domain.rule.model;

import java.time.LocalDateTime;

/**
 * 规则版本的时间坐标。两条时间轴正交：
 * validFrom/validTo 是业务生效时间（valid time），recordedAt 是系统记录时点（transaction time）。
 */
public record RuleVersionRef(
        long ruleId,
        String ruleKey,
        int versionSeq,
        LocalDateTime validFrom,
        LocalDateTime validTo,
        LocalDateTime recordedAt) {

    public RuleVersionRef {
        if (ruleKey == null || ruleKey.isBlank()) {
            throw new IllegalArgumentException("ruleKey is blank");
        }
        if (validFrom == null || recordedAt == null) {
            throw new IllegalArgumentException("validFrom and recordedAt are required");
        }
        if (validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    /** 业务上在该时点是否生效：[validFrom, validTo) */
    public boolean validAt(LocalDateTime bizTime) {
        if (bizTime == null) {
            return false;
        }
        return !bizTime.isBefore(validFrom) && (validTo == null || bizTime.isBefore(validTo));
    }

    /** 系统在该时点是否已经知道这条记录：recordedAt <= knownAt */
    public boolean knownAt(LocalDateTime knownAt) {
        if (knownAt == null) {
            return true;
        }
        return !recordedAt.isAfter(knownAt);
    }
}