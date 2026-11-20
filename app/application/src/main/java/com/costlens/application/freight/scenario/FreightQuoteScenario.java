package com.costlens.application.freight.scenario;

import com.costlens.client.freight.FreightApi;
import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.freight.service.FreightRatingService;
import com.costlens.domain.rule.model.FreightRateCard;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.rule.repository.FreightRateCardRepository;
import com.costlens.application.freight.assembler.FreightAssembler;
import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.model.Market;
import org.springframework.stereotype.Service;

/**
 * 运费试算场景：按市场选费率卡 → 解析业务时点版本 → 路由到策略计算。
 */
@Service
public class FreightQuoteScenario {

    private final FreightRateCardRepository cardRepository;
    private final FreightRatingService ratingService;
    private final FreightAssembler assembler;

    public FreightQuoteScenario(FreightRateCardRepository cardRepository,
                                FreightRatingService ratingService,
                                FreightAssembler assembler) {
        this.cardRepository = cardRepository;
        this.ratingService = ratingService;
        this.assembler = assembler;
    }

    public FreightApi.QuoteRes quote(FreightApi.QuoteReq req) {
        String rateKey = rateKeyOf(req);
        FreightRateCard card = cardRepository.findByKey(rateKey)
                .orElseThrow(() -> new RuleNotFoundException("未找到运费费率卡: rateKey=" + rateKey));

        FreightRateSnapshot snapshot = card.resolveAt(req.quoteTime()).snapshot();
        ShipmentContext ctx = assembler.toContext(req);
        FreightQuote quote = ratingService.quote(ctx, snapshot);
        return assembler.toRes(snapshot, quote);
    }

    /** 费率卡标识按承运商归一；一个承运商对一份运价卡 */
    private String rateKeyOf(FreightApi.QuoteReq req) {
        if (req.carrier() != null) {
            return req.carrier().name();
        }
        return switch (req.market()) {
            case US -> "UPS_GROUND";
            case JP -> "YAMATO_TAKKYUUBIN";
            case SEA -> "SHOPEE_SLS";
        };
    }

    public Market marketOf(FreightApi.QuoteReq req) {
        return req.market();
    }
}