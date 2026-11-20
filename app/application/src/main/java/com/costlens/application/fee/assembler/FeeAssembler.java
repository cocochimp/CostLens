package com.costlens.application.fee.assembler;

import com.costlens.client.fee.FeeApi;
import com.costlens.domain.fee.model.ChargeContext;
import com.costlens.domain.fee.model.FeeChargeResult;
import com.costlens.domain.shared.model.Money;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 费用计提对外契约 ↔ 领域对象转换。
 */
@Component
public class FeeAssembler {

    public ChargeContext toContext(FeeApi.ChargeReq req) {
        return ChargeContext.of(
                Money.ofMinor(req.baseAmountMinor(), req.currency()),
                req.pieceCount(), req.volumeCubicCm(), req.weightGrams());
    }

    public FeeApi.ChargeRes toRes(FeeChargeResult result, com.costlens.model.Currency currency) {
        List<FeeApi.ChargeItemRes> items = result.items().stream()
                .map(i -> new FeeApi.ChargeItemRes(i.costType(), i.ruleType(),
                        i.ruleKey(), i.ruleVersionSeq(), i.amount().minorUnits(), i.trace()))
                .toList();
        return new FeeApi.ChargeRes(items, result.total().minorUnits(), currency);
    }
}