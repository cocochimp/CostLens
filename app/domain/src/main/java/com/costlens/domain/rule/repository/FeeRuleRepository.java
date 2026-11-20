package com.costlens.domain.rule.repository;

import com.costlens.domain.rule.model.FeeRule;
import com.costlens.model.Platform;

import java.util.List;
import java.util.Optional;

/**
 * 费用规则仓储接口。实现放在 infrastructure，依赖倒置。
 */
public interface FeeRuleRepository {

    Optional<FeeRule> findByKey(String ruleKey);

    List<FeeRule> findByPlatformAndCategory(Platform platform, String category);

    void save(FeeRule rule);
}