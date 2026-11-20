package com.costlens.domain.fee.service;

import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.fee.model.CostItem;
import com.costlens.domain.fee.model.FeeChargeResult;
import com.costlens.domain.fee.service.rule.ChargeRule;
import com.costlens.domain.fee.service.rule.ChargeRuleFactory;
import com.costlens.domain.rule.model.FeeRuleVersion;
import com.costlens.domain.shared.model.Money;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 费用计提服务（规则+计算模式）：互斥筛选 → 逐项计算 → 汇总。
 * 逐项不合并，因为合并会同时丢精度与可解释性。
 */
public class FeeChargeService {

    public FeeChargeResult charge(ChargeContext ctx, List<FeeRuleVersion> versions) {
        List<FeeRuleVersion> effective = selectEffective(versions);

        List<CostItem> items = new ArrayList<>(effective.size());
        for (FeeRuleVersion version : effective) {
            ChargeRule rule = ChargeRuleFactory.of(version.params());
            Money amount = rule.charge(ctx);
            Map<String, Object> trace = new HashMap<>();
            trace.put("baseAmount", ctx.baseAmount().toDecimal().toPlainString());
            trace.put("ruleType", version.params().ruleType().name());
            if (version.params().floor() != null) {
                trace.put("floor", version.params().floor().toDecimal().toPlainString());
            }
            items.add(new CostItem(version.params().costType(), version.params().ruleType().name(),
                    version.ref().ruleKey(), version.ref().versionSeq(), amount, trace));
        }

        Money total = items.stream()
                .map(CostItem::amount)
                .reduce(Money.zero(ctx.baseAmount().currency()), Money::plus);

        return new FeeChargeResult(items, total);
    }

    /**
     * 互斥组筛选：同一 mutexGroup 内只保留 priority 最高的一条。
     * 典型场景：参加活动免佣金 与 活动服务费 不可同时生效。
     */
    private List<FeeRuleVersion> selectEffective(List<FeeRuleVersion> versions) {
        List<FeeRuleVersion> noGroup = new ArrayList<>();
        Map<String, FeeRuleVersion> winners = new HashMap<>();

        for (FeeRuleVersion version : versions) {
            String group = version.params().mutexGroup();
            if (group == null || group.isBlank()) {
                noGroup.add(version);
                continue;
            }
            FeeRuleVersion current = winners.get(group);
            if (current == null || version.params().priority() > current.params().priority()) {
                winners.put(group, version);
            }
        }

        List<FeeRuleVersion> effective = new ArrayList<>(noGroup);
        effective.addAll(winners.values());
        return effective;
    }
}