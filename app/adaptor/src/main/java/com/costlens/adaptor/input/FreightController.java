package com.costlens.adaptor.input;

import com.costlens.application.freight.scenario.FreightQuoteScenario;
import com.costlens.client.freight.FreightApi;
import com.costlens.common.result.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/freight")
public class FreightController {

    private final FreightQuoteScenario scenario;

    public FreightController(FreightQuoteScenario scenario) {
        this.scenario = scenario;
    }

    @PostMapping("/quote")
    public Result<FreightApi.QuoteRes> quote(@RequestBody FreightApi.QuoteReq req) {
        return Result.ok(scenario.quote(req));
    }
}