package com.costlens.domain.rule.model;

import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.common.exception.RuleVersionConflictException;
import com.costlens.model.CostType;
import com.costlens.model.Platform;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 费用规则聚合根（写模式）。
 * 一条规则身份下挂多个版本，版本只追加不修改，因此历史认知状态可复现。
 */
public class FeeRule {

    private final String ruleKey;
    private final Platform platform;
    private final String category;
    private final CostType costType;
    private final List<FeeRuleVersion> versions;

    public FeeRule(String ruleKey, Platform platform, String category, CostType costType) {
        this(ruleKey, platform, category, costType, new ArrayList<>());
    }

    public FeeRule(String ruleKey, Platform platform, String category, CostType costType,
                   List<FeeRuleVersion> versions) {
        if (ruleKey == null || ruleKey.isBlank()) {
            throw new IllegalArgumentException("ruleKey is blank");
        }
        this.ruleKey = ruleKey;
        this.platform = platform;
        this.category = category == null ? "ALL" : category;
        this.costType = costType;
        this.versions = new ArrayList<>(versions == null ? List.of() : versions);
    }

    public String ruleKey() {
        return ruleKey;
    }

    public Platform platform() {
        return platform;
    }

    public String category() {
        return category;
    }

    public CostType costType() {
        return costType;
    }

    public List<FeeRuleVersion> versions() {
        return List.copyOf(versions);
    }

    /** 业务时点解析：日常核算口径 */
    public FeeRuleVersion resolveAt(LocalDateTime bizTime) {
        return versions.stream()
                .filter(v -> v.ref().validAt(bizTime))
                .max(Comparator.comparingInt(v -> v.ref().versionSeq()))
                .orElseThrow(() -> new RuleNotFoundException(ruleKey, bizTime));
    }

    /** 认知时点解析：复现「当时系统认为适用哪条规则」 */
    public FeeRuleVersion resolveAsOf(LocalDateTime bizTime, LocalDateTime knownAt) {
        return versions.stream()
                .filter(v -> v.ref().knownAt(knownAt))
                .filter(v -> v.ref().validAt(bizTime))
                .max(Comparator.comparing(v -> v.ref().recordedAt()))
                .orElseThrow(() -> new RuleNotFoundException(
                        "ruleKey=" + ruleKey + ", bizTime=" + bizTime + ", knownAt=" + knownAt));
    }

    public Optional<FeeRuleVersion> latestRecordedBefore(LocalDateTime knownAt) {
        return versions.stream()
                .filter(v -> v.ref().knownAt(knownAt))
                .max(Comparator.comparing(v -> v.ref().recordedAt()));
    }

    /**
     * 追加新版本。两条不变量：
     * 1. recordedAt 必须严格晚于该规则已有全部记录时点；
     * 2. 同一 recordedAt 视角下，valid 区间不得重叠（不同 recordedAt 允许重叠——那正是认知变更）。
     */
    public FeeRuleVersion appendVersion(FeeRuleParams params,
                                        LocalDateTime validFrom,
                                        LocalDateTime validTo,
                                        LocalDateTime recordedAt) {
        LocalDateTime maxRecorded = versions.stream()
                .map(v -> v.ref().recordedAt())
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
        if (!recordedAt.isAfter(maxRecorded)) {
            throw new RuleVersionConflictException(ruleKey, recordedAt, maxRecorded);
        }

        for (FeeRuleVersion existing : versions) {
            if (!existing.ref().recordedAt().equals(recordedAt)) {
                continue;
            }
            if (overlaps(validFrom, validTo, existing.ref().validFrom(), existing.ref().validTo())) {
                throw new RuleVersionConflictException(
                        "同一记录时点下 valid 区间重叠: ruleKey=" + ruleKey
                                + ", existing=" + existing.ref().validFrom() + "~" + existing.ref().validTo());
            }
        }

        RuleVersionRef ref = new RuleVersionRef(
                0L, ruleKey, versions.size() + 1, validFrom, validTo, recordedAt);
        FeeRuleVersion version = new FeeRuleVersion(ref, params);
        versions.add(version);
        return version;
    }

    private static boolean overlaps(LocalDateTime aFrom, LocalDateTime aTo,
                                    LocalDateTime bFrom, LocalDateTime bTo) {
        boolean aEndsBeforeB = aTo != null && !aTo.isAfter(bFrom);
        boolean bEndsBeforeA = bTo != null && !bTo.isAfter(aFrom);
        return !aEndsBeforeB && !bEndsBeforeA;
    }
}