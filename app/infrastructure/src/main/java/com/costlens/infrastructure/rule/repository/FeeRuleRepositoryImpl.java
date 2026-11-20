package com.costlens.infrastructure.rule.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.costlens.domain.rule.model.FeeRule;
import com.costlens.domain.rule.model.FeeRuleParams;
import com.costlens.domain.rule.model.FeeRuleVersion;
import com.costlens.domain.rule.model.RuleVersionRef;
import com.costlens.domain.rule.repository.FeeRuleRepository;
import com.costlens.infrastructure.rule.mysql.FeeRuleVersionMapper;
import com.costlens.infrastructure.rule.mysql.po.FeeRuleVersionPO;
import com.costlens.infrastructure.shared.JsonCodec;
import com.costlens.model.CostType;
import com.costlens.model.Platform;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 费用规则仓储实现。写入只做 INSERT，绝不 UPDATE 历史版本。
 */
@Repository
public class FeeRuleRepositoryImpl implements FeeRuleRepository {

    private final FeeRuleVersionMapper mapper;
    private final JsonCodec codec;

    public FeeRuleRepositoryImpl(FeeRuleVersionMapper mapper, JsonCodec codec) {
        this.mapper = mapper;
        this.codec = codec;
    }

    @Override
    public Optional<FeeRule> findByKey(String ruleKey) {
        List<FeeRuleVersionPO> rows = mapper.selectList(
                Wrappers.<FeeRuleVersionPO>lambdaQuery()
                        .eq(FeeRuleVersionPO::getRuleKey, ruleKey)
                        .orderByAsc(FeeRuleVersionPO::getVersionSeq));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toDomain(ruleKey, rows));
    }

    @Override
    public List<FeeRule> findByPlatformAndCategory(Platform platform, String category) {
        List<FeeRuleVersionPO> rows = mapper.selectList(
                Wrappers.<FeeRuleVersionPO>lambdaQuery()
                        .eq(FeeRuleVersionPO::getPlatform, platform.name())
                        .and(w -> w.eq(FeeRuleVersionPO::getCategory, category)
                                .or().eq(FeeRuleVersionPO::getCategory, "ALL"))
                        .orderByAsc(FeeRuleVersionPO::getRuleKey)
                        .orderByAsc(FeeRuleVersionPO::getVersionSeq));
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<String, List<FeeRuleVersionPO>> grouped = rows.stream()
                .collect(Collectors.groupingBy(FeeRuleVersionPO::getRuleKey));
        return grouped.entrySet().stream()
                .map(e -> toDomain(e.getKey(), e.getValue()))
                .toList();
    }

    @Override
    public void save(FeeRule rule) {
        for (FeeRuleVersion version : rule.versions()) {
            Long existing = mapper.selectCount(Wrappers.<FeeRuleVersionPO>lambdaQuery()
                    .eq(FeeRuleVersionPO::getRuleKey, rule.ruleKey())
                    .eq(FeeRuleVersionPO::getVersionSeq, version.ref().versionSeq()));
            if (existing != null && existing > 0) {
                continue;
            }
            mapper.insert(toPO(rule, version));
        }
    }

    private FeeRule toDomain(String ruleKey, List<FeeRuleVersionPO> rows) {
        FeeRuleVersionPO first = rows.get(0);
        List<FeeRuleVersion> versions = rows.stream().map(this::toVersion).toList();
        return new FeeRule(ruleKey,
                Platform.valueOf(first.getPlatform()),
                first.getCategory(),
                CostType.valueOf(first.getCostType()),
                versions);
    }

    private FeeRuleVersion toVersion(FeeRuleVersionPO po) {
        FeeRuleParams params = codec.read(po.getParamsJson(), FeeRuleParams.class);
        RuleVersionRef ref = new RuleVersionRef(po.getId(), po.getRuleKey(), po.getVersionSeq(),
                po.getValidFrom(), po.getValidTo(), po.getRecordedAt());
        return new FeeRuleVersion(ref, params);
    }

    private FeeRuleVersionPO toPO(FeeRule rule, FeeRuleVersion version) {
        FeeRuleVersionPO po = new FeeRuleVersionPO();
        po.setRuleKey(rule.ruleKey());
        po.setVersionSeq(version.ref().versionSeq());
        po.setPlatform(rule.platform().name());
        po.setCategory(rule.category());
        po.setCostType(version.params().costType().name());
        po.setRuleType(version.params().ruleType().name());
        po.setParamsJson(codec.write(version.params()));
        // 规则自身不带币种，按平台结算币种落库；跨币种规则需在 params 显式约定
        po.setCurrency(rule.platform().defaultCurrency().name());
        po.setValidFrom(version.ref().validFrom());
        po.setValidTo(version.ref().validTo());
        po.setRecordedAt(version.ref().recordedAt());
        return po;
    }
}