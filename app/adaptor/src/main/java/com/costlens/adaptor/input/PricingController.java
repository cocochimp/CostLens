package com.costlens.adaptor.input;

import com.costlens.application.pricing.scenario.PricingScenario;
import com.costlens.client.pricing.PricingApi;
import com.costlens.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingScenario scenario;

    public PricingController(PricingScenario scenario) {
        this.scenario = scenario;
    }

    @PostMapping("/solve")
    public Result<PricingApi.SolveRes> solve(@RequestBody PricingApi.SolveReq req) {
        return Result.ok(scenario.solve(req));
    }
}