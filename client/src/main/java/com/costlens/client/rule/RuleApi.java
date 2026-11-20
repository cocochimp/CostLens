package com.costlens.client.rule;

import com.costlens.model.ChargeRuleType;
import com.costlens.model.CostType;
import com.costlens.model.MeasureKind;
import com.costlens.model.Platform;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则双时态查询与版本追加对外契约。
 */
public final class RuleApi {

    private RuleApi() {
    }

    public record AsOfRes(
            String ruleKey,
            int versionSeq,
            String validFrom,
            String validTo,
            String recordedAt,
            String paramsJson) {
    }

    public record AmountTierSpec(
            long lowerInclusiveMinor,
            Long upperExclusiveMinor,
            String ratePercent) {
    }

    public record AppendVersionReq(
            Platform platform,
            String category,
            CostType costType,
            ChargeRuleType ruleType,
            String ratePercent,
            Long floorMinor,
            List<AmountTierSpec> tiers,
            MeasureKind measureKind,
            Long unitPriceMinor,
            Long capMinor,
            String mutexGroup,
            Integer priority,
            LocalDateTime validFrom,
            LocalDateTime validTo,
            LocalDateTime recordedAt) {
    }

    public record AppendVersionRes(
            String ruleKey,
            int versionSeq,
            String recordedAt) {
    }
}