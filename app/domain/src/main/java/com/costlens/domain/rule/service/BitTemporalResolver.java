package com.costlens.domain.rule.service;

import com.costlens.domain.rule.model.RuleVersionRef;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 双时态解析器。纯计算，无状态。
 */
public final class BitTemporalResolver {

    /**
     * 业务时点查询：只看 valid time，取版本序号最大的命中者。
     * 用于日常核算。
     */
    public Optional<RuleVersionRef> atBusinessTime(List<RuleVersionRef> versions, LocalDateTime bizTime) {
        if (versions == null || versions.isEmpty() || bizTime == null) {
            return Optional.empty();
        }
        return versions.stream()
                .filter(v -> v.validAt(bizTime))
                .max(Comparator.comparingInt(RuleVersionRef::versionSeq));
    }

    /**
     * 认知时点查询（as-of）：同时约束 valid time 与 transaction time，
     * 取"截至 knownAt 系统已知道的最新一条"。
     * 用于复现历史决策依据。
     */
    public Optional<RuleVersionRef> asOf(List<RuleVersionRef> versions,
                                         LocalDateTime bizTime,
                                         LocalDateTime knownAt) {
        if (versions == null || versions.isEmpty() || bizTime == null) {
            return Optional.empty();
        }
        return versions.stream()
                .filter(v -> v.knownAt(knownAt))
                .filter(v -> v.validAt(bizTime))
                .max(Comparator.comparing(RuleVersionRef::recordedAt));
    }
}