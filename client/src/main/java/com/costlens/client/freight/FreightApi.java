package com.costlens.client.freight;

import com.costlens.model.Carrier;
import com.costlens.model.Currency;
import com.costlens.model.Market;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 运费试算对外契约。
 */
public final class FreightApi {

    private FreightApi() {
    }

    public record QuoteReq(
            Market market,
            Carrier carrier,
            String serviceLevel,
            String destZip,
            String area,
            String country,
            long actualWeightGram,
            int lengthCm,
            int widthCm,
            int heightCm,
            Currency currency,
            LocalDateTime quoteTime) {
    }

    public record QuoteRes(
            String rateKey,
            int rateVersionSeq,
            long billedWeightGram,
            Long dimWeightGram,
            Integer zone,
            Integer girthCm,
            String sizeBand,
            Long continuationUnits,
            long baseFreightMinor,
            long surchargeMinor,
            long totalMinor,
            Currency currency,
            Map<String, Object> trace) {
    }
}