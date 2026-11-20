package com.costlens.domain.freight.service;

import com.costlens.common.exception.UnsupportedMarketException;
import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.freight.service.strategy.FreightRateStrategy;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.model.Market;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 运费计费服务（规则+计算模式）：按市场路由到策略，本身不含计价逻辑。
 * 新增市场 = 新增一个策略实现并注册，不改本类。
 */
public class FreightRatingService {

    private final Map<Market, FreightRateStrategy> strategies = new EnumMap<>(Market.class);

    public FreightRatingService(List<FreightRateStrategy> strategyList) {
        for (FreightRateStrategy strategy : strategyList) {
            strategies.put(strategy.market(), strategy);
        }
    }

    public FreightQuote quote(ShipmentContext ctx, FreightRateSnapshot snapshot) {
        FreightRateStrategy strategy = strategies.get(ctx.market());
        if (strategy == null) {
            throw new UnsupportedMarketException(ctx.market().name());
        }
        if (snapshot.market() != ctx.market()) {
            throw new IllegalArgumentException(
                    "费率卡市场与上下文不一致: snapshot=" + snapshot.market() + ", ctx=" + ctx.market());
        }
        return strategy.quote(ctx, snapshot);
    }

    public java.util.Set<Market> supportedMarkets() {
        return strategies.keySet();
    }
}