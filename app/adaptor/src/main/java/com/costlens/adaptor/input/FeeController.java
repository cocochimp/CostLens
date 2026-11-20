package com.costlens.adaptor.input;

import com.costlens.application.fee.scenario.FeeChargeScenario;
import com.costlens.client.fee.FeeApi;
import com.costlens.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fee")
public class FeeController {

    private final FeeChargeScenario scenario;

    public FeeController(FeeChargeScenario scenario) {
        this.scenario = scenario;
    }

    @PostMapping("/charge")
    public Result<FeeApi.ChargeRes> charge(@RequestBody FeeApi.ChargeReq req) {
        return Result.ok(scenario.charge(req));
    }
}