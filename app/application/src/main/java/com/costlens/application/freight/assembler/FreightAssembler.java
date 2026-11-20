package com.costlens.application.freight.assembler;

import com.costlens.client.freight.FreightApi;
import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.shared.model.Length3;
import com.costlens.domain.shared.model.Weight;
import org.springframework.stereotype.Component;

/**
 * 运费对外契约 ↔ 领域对象转换。
 */
@Component
public class FreightAssembler {

    public ShipmentContext toContext(FreightApi.QuoteReq req) {
        return ShipmentContext.builder(req.market(), req.currency())
                .carrier(req.carrier())
                .serviceLevel(req.serviceLevel() == null ? "GROUND" : req.serviceLevel())
                .destZip(req.destZip())
                .area(req.area())
                .country(req.country())
                .actualWeight(Weight.ofGrams(req.actualWeightGram()))
                .dimensions(new Length3(req.lengthCm(), req.widthCm(), req.heightCm()))
                .build();
    }

    public FreightApi.QuoteRes toRes(FreightRateSnapshot snapshot, FreightQuote quote) {
        return new FreightApi.QuoteRes(
                snapshot.rateKey(),
                snapshot.versionSeq(),
                quote.billedWeightGram(),
                quote.dimWeightGram(),
                quote.zone(),
                quote.girthCm(),
                quote.sizeBand() == null ? null : quote.sizeBand().name(),
                quote.continuationUnits(),
                quote.baseFreight().minorUnits(),
                quote.surcharge().minorUnits(),
                quote.total().minorUnits(),
                quote.currency(),
                quote.trace());
    }
}