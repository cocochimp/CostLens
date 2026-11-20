package com.costlens.application.pricing.scenario;

import com.costlens.client.pricing.PricingApi;
import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.domain.fee.service.rule.ChargeRule;
import com.costlens.domain.fee.service.rule.ChargeRuleFactory;
import com.costlens.domain.pricing.model.PricingInput;
import com.costlens.domain.pricing.model.PricingSolution;
import com.costlens.domain.pricing.service.PricingSolver;
import com.costlens.domain.rule.model.FeeRule;
import com.costlens.domain.rule.model.FeeRuleVersion;
import com.costlens.domain.rule.repository.FeeRuleRepository;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.CostType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 定价求解场景：取依赖售价的费用规则 → 交给分段方程求解器。
 * 只有佣金与支付手续费随售价变动，仓储/广告属于固定成本，由调用方并入 fixedCost。
 */
@Service
public class PricingScenario {

    private final FeeRuleRepository ruleRepository;
    private final PricingSolver solver;

    public PricingScenario(FeeRuleRepository ruleRepository, PricingSolver solver) {
        this.ruleRepository = ruleRepository;
        this.solver = solver;
    }

    public PricingApi.SolveRes solve(PricingApi.SolveReq req) {
        List<FeeRule> rules = ruleRepository.findByPlatformAndCategory(req.platform(), req.category());
        LocalDateTime asOf = LocalDateTime.now();

        List<ChargeRule> chargeRules = rules.stream()
                .filter(r -> r.costType() == CostType.COMMISSION || r.costType() == CostType.PAYMENT)
                .map(r -> resolve(r, asOf))
                .filter(java.util.Objects::nonNull)
                .map(v -> ChargeRuleFactory.of(v.params()))
                .toList();

        PricingInput input = new PricingInput(
                req.currency(),
                Money.ofMinor(req.fixedCostMinor(), req.currency()),
                Rate.ofPercent(req.targetMarginPercent()),
                req.priceCapMinor() == null ? null : Money.ofMinor(req.priceCapMinor(), req.currency()),
                chargeRules,
                req.pieceCount(),
                req.volumeCubicCm(),
                req.weightGrams());

        PricingSolution solution = solver.solve(input);
        return toRes(solution);
    }

    private FeeRuleVersion resolve(FeeRule rule, LocalDateTime asOf) {
        try {
            return rule.resolveAt(asOf);
        } catch (RuleNotFoundException e) {
            return null;
        }
    }

    private PricingApi.SolveRes toRes(PricingSolution solution) {
        return new PricingApi.SolveRes(
                solution.feasible(),
                solution.solvedPrice() == null ? null : solution.solvedPrice().minorUnits(),
                solution.priceCap() == null ? null : solution.priceCap().minorUnits(),
                solution.gap() == null ? null : solution.gap().minorUnits(),
                solution.reason().name(),
                solution.message(),
                solution.trace());
    }
}