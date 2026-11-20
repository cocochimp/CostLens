package com.costlens.application.cost.scenario;

import com.costlens.application.cost.assembler.CostAssembler;
import com.costlens.application.fee.scenario.FeeChargeScenario;
import com.costlens.application.freight.scenario.FreightQuoteScenario;
import com.costlens.client.cost.CostApi;
import com.costlens.client.fee.FeeApi;
import com.costlens.client.freight.FreightApi;
import com.costlens.domain.cost.model.OrderCostSheet;
import com.costlens.domain.cost.repository.CostSheetRepository;
import com.costlens.domain.fee.model.CostItem;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.CostType;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 成本单生成场景：运费 + 平台费用 + 固定成本 → 成本单 → 确认落库。
 * 所有成本项必须同币种，跨币种不做隐式换算。
 */
@Service
public class CostSheetScenario {

    private final FreightQuoteScenario freightScenario;
    private final FeeChargeScenario feeScenario;
    private final CostSheetRepository sheetRepository;
    private final com.costlens.application.cost.port.CostSheetNotifier notifier;
    private final CostAssembler assembler;

    public CostSheetScenario(FreightQuoteScenario freightScenario,
                             FeeChargeScenario feeScenario,
                             CostSheetRepository sheetRepository,
                             com.costlens.application.cost.port.CostSheetNotifier notifier,
                             CostAssembler assembler) {
        this.freightScenario = freightScenario;
        this.feeScenario = feeScenario;
        this.sheetRepository = sheetRepository;
        this.notifier = notifier;
        this.assembler = assembler;
    }

    public CostApi.SheetRes generate(CostApi.SheetReq req) {
        Money revenue = Money.ofMinor(req.revenueMinor(), req.currency());
        OrderCostSheet sheet = new OrderCostSheet(req.sheetId(), req.orderNo(), req.platform(), revenue);

        addFreight(sheet, req);
        addPlatformFees(sheet, req);
        addFixedCosts(sheet, req);

        sheet.confirm();
        sheetRepository.save(sheet);
        notifier.notifyConfirmed(sheet.sheetId(), sheet.orderNo(),
                sheet.totalCost().minorUnits(), sheet.revenue().currency().name());
        return assembler.toSheetRes(sheet);
    }

    /** 查询成本单（读模式，不做任何业务计算） */
    public CostApi.SheetRes query(String sheetId) {
        OrderCostSheet sheet = sheetRepository.findById(sheetId)
                .orElseThrow(() -> new com.costlens.common.exception.CostLensException(
                        com.costlens.common.exception.ErrorCodes.INVALID_INPUT,
                        "成本单不存在: " + sheetId));
        return assembler.toSheetRes(sheet);
    }

    private void addFreight(OrderCostSheet sheet, CostApi.SheetReq req) {
        if (req.freight() == null) {
            return;
        }
        CostApi.FreightSpec spec = req.freight();
        FreightApi.QuoteReq quoteReq = new FreightApi.QuoteReq(
                spec.market(), null, spec.serviceLevel(), spec.destZip(), spec.area(), spec.country(),
                spec.actualWeightGram(), spec.lengthCm(), spec.widthCm(), spec.heightCm(),
                req.currency(), req.bizTime());

        FreightApi.QuoteRes quote = freightScenario.quote(quoteReq);
        sheet.addItem(new CostItem(CostType.FREIGHT, "FREIGHT", quote.rateKey(),
                quote.rateVersionSeq(),
                Money.ofMinor(quote.totalMinor(), req.currency()),
                Map.of("billedWeightGram", quote.billedWeightGram(),
                        "zone", quote.zone() == null ? "" : quote.zone(),
                        "sizeBand", quote.sizeBand() == null ? "" : quote.sizeBand())));
    }

    private void addPlatformFees(OrderCostSheet sheet, CostApi.SheetReq req) {
        FeeApi.ChargeReq chargeReq = new FeeApi.ChargeReq(req.platform(), req.category(),
                req.revenueMinor(), req.currency(), 1, 0L, 0L, req.bizTime());

        FeeApi.ChargeRes fees = feeScenario.charge(chargeReq);
        for (FeeApi.ChargeItemRes item : fees.items()) {
            sheet.addItem(new CostItem(item.costType(), item.ruleType(), item.ruleKey(),
                    item.ruleVersionSeq(), Money.ofMinor(item.amountMinor(), req.currency()), item.trace()));
        }
    }

    private void addFixedCosts(OrderCostSheet sheet, CostApi.SheetReq req) {
        if (req.fixedCosts() == null) {
            return;
        }
        for (CostApi.FixedCostSpec spec : req.fixedCosts()) {
            sheet.addItem(new CostItem(spec.costType(), "FIXED",
                    spec.ruleKey() == null ? "MANUAL" : spec.ruleKey(), 0,
                    Money.ofMinor(spec.amountMinor(), req.currency()),
                    Map.of("source", "manual")));
        }
    }
}