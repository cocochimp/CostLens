package com.costlens.application.rule.scenario;

import com.costlens.client.rule.RuleApi;
import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.domain.rule.model.AmountTier;
import com.costlens.domain.rule.model.FeeRule;
import com.costlens.domain.rule.model.FeeRuleParams;
import com.costlens.domain.rule.model.FeeRuleVersion;
import com.costlens.domain.rule.repository.FeeRuleRepository;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.ChargeRuleType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 规则双时态查询与版本追加场景。
 */
@Service
public class RuleScenario {

    private final FeeRuleRepository ruleRepository;

    public RuleScenario(FeeRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /** as-of 查询：同时约束业务时间与系统认知时间 */
    public RuleApi.AsOfRes asOf(String ruleKey, LocalDateTime bizTime, LocalDateTime knownAt) {
        FeeRule rule = ruleRepository.findByKey(ruleKey)
                .orElseThrow(() -> new RuleNotFoundException("规则不存在: " + ruleKey));

        FeeRuleVersion version = knownAt == null
                ? rule.resolveAt(bizTime)
                : rule.resolveAsOf(bizTime, knownAt);

        return new RuleApi.AsOfRes(
                ruleKey,
                version.ref().versionSeq(),
                String.valueOf(version.ref().validFrom()),
                String.valueOf(version.ref().validTo()),
                String.valueOf(version.ref().recordedAt()),
                String.valueOf(version.params().ruleType()));
    }

    /** 追加新版本。规则只追加不修改，修正同样走追加 */
    public RuleApi.AppendVersionRes appendVersion(RuleApi.AppendVersionReq req) {
        String ruleKey = ruleKeyOf(req);
        FeeRule rule = ruleRepository.findByKey(ruleKey)
                .orElseGet(() -> new FeeRule(ruleKey, req.platform(), req.category(), req.costType()));

        FeeRuleParams params = toParams(req);
        FeeRuleVersion version = rule.appendVersion(params,
                req.validFrom(), req.validTo(),
                req.recordedAt() == null ? LocalDateTime.now() : req.recordedAt());

        ruleRepository.save(rule);
        return new RuleApi.AppendVersionRes(ruleKey, version.ref().versionSeq(),
                String.valueOf(version.ref().recordedAt()));
    }

    private String ruleKeyOf(RuleApi.AppendVersionReq req) {
        String category = req.category() == null || req.category().isBlank() ? "ALL" : req.category();
        return req.platform().name() + "_" + category + "_" + req.costType().name();
    }

    private FeeRuleParams toParams(RuleApi.AppendVersionReq req) {
        ChargeRuleType type = req.ruleType();
        FeeRuleParams params = switch (type) {
            case PERCENTAGE -> FeeRuleParams.percentage(req.costType(),
                    Rate.ofPercent(require(req.ratePercent(), "ratePercent")));
            case PERCENTAGE_FLOOR -> FeeRuleParams.percentageWithFloor(req.costType(),
                    Rate.ofPercent(require(req.ratePercent(), "ratePercent")),
                    Money.ofMinor(require(req.floorMinor(), "floorMinor"), req.platform().defaultCurrency()));
            case TIERED_BY_AMOUNT -> FeeRuleParams.tieredByAmount(req.costType(), toTiers(req));
            case PER_UNIT_BY_MEASURE -> FeeRuleParams.perUnit(req.costType(),
                    require(req.measureKind(), "measureKind"),
                    Money.ofMinor(require(req.unitPriceMinor(), "unitPriceMinor"),
                            req.platform().defaultCurrency()));
        };

        if (req.capMinor() != null) {
            params = params.withCap(Money.ofMinor(req.capMinor(), req.platform().defaultCurrency()));
        }
        if (req.mutexGroup() != null) {
            params = params.withMutex(req.mutexGroup(), req.priority() == null ? 0 : req.priority());
        }
        return params;
    }

    private List<AmountTier> toTiers(RuleApi.AppendVersionReq req) {
        if (req.tiers() == null || req.tiers().isEmpty()) {
            throw new IllegalArgumentException("TIERED_BY_AMOUNT 需要 tiers");
        }
        var currency = req.platform().defaultCurrency();
        return req.tiers().stream()
                .map(t -> new AmountTier(
                        Money.ofMinor(t.lowerInclusiveMinor(), currency),
                        t.upperExclusiveMinor() == null ? null : Money.ofMinor(t.upperExclusiveMinor(), currency),
                        Rate.ofPercent(t.ratePercent())))
                .toList();
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("缺少必填字段: " + field);
        }
        return value;
    }
}