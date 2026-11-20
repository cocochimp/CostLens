package com.costlens.application.fee.scenario;

import com.costlens.application.fee.assembler.FeeAssembler;
import com.costlens.client.fee.FeeApi;
import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.fee.model.FeeChargeResult;
import com.costlens.domain.fee.service.FeeChargeService;
import com.costlens.domain.rule.model.FeeRule;
import com.costlens.domain.rule.model.FeeRuleVersion;
import com.costlens.domain.rule.repository.FeeRuleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 费用计提场景：按平台与类目取规则 → 解析业务时点版本 → 逐项计提汇总。
 * 规则在请求时点尚未生效则跳过；一条都没有则明确失败，不回退默认值。
 */
@Service
public class FeeChargeScenario {

    private final FeeRuleRepository ruleRepository;
    private final FeeChargeService chargeService;
    private final FeeAssembler assembler;

    public FeeChargeScenario(FeeRuleRepository ruleRepository,
                             FeeChargeService chargeService,
                             FeeAssembler assembler) {
        this.ruleRepository = ruleRepository;
        this.chargeService = chargeService;
        this.assembler = assembler;
    }

    public FeeApi.ChargeRes charge(FeeApi.ChargeReq req) {
        List<FeeRule> rules = ruleRepository.findByPlatformAndCategory(req.platform(), req.category());

        List<FeeRuleVersion> versions = new ArrayList<>();
        for (FeeRule rule : rules) {
            try {
                versions.add(rule.resolveAt(req.chargeTime()));
            } catch (RuleNotFoundException ignored) {
                // 该规则在请求时点尚未生效，属于正常情况，跳过
            }
        }
        if (versions.isEmpty()) {
            throw new RuleNotFoundException("平台=" + req.platform() + ", 类目=" + req.category()
                    + " 在 " + req.chargeTime() + " 无任何生效的费用规则");
        }

        ChargeContext ctx = assembler.toContext(req);
        FeeChargeResult result = chargeService.charge(ctx, versions);
        return assembler.toRes(result, req.currency());
    }
}