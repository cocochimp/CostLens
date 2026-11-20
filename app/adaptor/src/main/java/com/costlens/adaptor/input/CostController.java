package com.costlens.adaptor.input;

import com.costlens.application.cost.scenario.CostSheetScenario;
import com.costlens.application.cost.scenario.RecalcScenario;
import com.costlens.client.cost.CostApi;
import com.costlens.common.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cost")
public class CostController {

    private final CostSheetScenario sheetScenario;
    private final RecalcScenario recalcScenario;

    public CostController(CostSheetScenario sheetScenario, RecalcScenario recalcScenario) {
        this.sheetScenario = sheetScenario;
        this.recalcScenario = recalcScenario;
    }

    @PostMapping("/sheet")
    public Result<CostApi.SheetRes> generate(@RequestBody CostApi.SheetReq req) {
        return Result.ok(sheetScenario.generate(req));
    }

    @GetMapping("/sheet/{sheetId}")
    public Result<CostApi.SheetRes> getSheet(@PathVariable String sheetId) {
        return Result.ok(sheetScenario.query(sheetId));
    }

    @PostMapping("/recalc")
    public Result<CostApi.RecalcRes> createRecalc(@RequestBody CostApi.RecalcReq req) {
        return Result.ok(recalcScenario.create(req));
    }

    @GetMapping("/recalc/{batchId}")
    public Result<CostApi.RecalcRes> getRecalc(@PathVariable String batchId) {
        return Result.ok(recalcScenario.query(batchId));
    }

    @PostMapping("/recalc/{batchId}/shard")
    public Result<CostApi.RecalcRes> reportShard(@PathVariable String batchId,
                                                 @RequestParam int shardId,
                                                 @RequestParam boolean success) {
        return Result.ok(recalcScenario.reportShard(batchId, shardId, success));
    }
}