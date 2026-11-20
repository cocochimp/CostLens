package com.costlens.adaptor.input;

import com.costlens.application.rule.scenario.RuleScenario;
import com.costlens.client.rule.RuleApi;
import com.costlens.common.result.Result;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/rule")
public class RuleController {

    private final RuleScenario scenario;

    public RuleController(RuleScenario scenario) {
        this.scenario = scenario;
    }

    /**
     * 双时态查询：bizTime 是业务时间，knownAt 是系统认知时点。
     * 只传 bizTime 走日常核算口径；两者都传可复现历史决策依据。
     */
    @GetMapping("/{ruleKey}/as-of")
    public Result<RuleApi.AsOfRes> asOf(
            @PathVariable String ruleKey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime bizTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime knownAt) {
        return Result.ok(scenario.asOf(ruleKey, bizTime, knownAt));
    }

    @PostMapping("/versions")
    public Result<RuleApi.AppendVersionRes> appendVersion(@RequestBody RuleApi.AppendVersionReq req) {
        return Result.ok(scenario.appendVersion(req));
    }
}