package com.costlens.domain.freight.service.strategy;

import com.costlens.domain.freight.model.FreightQuote;
import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.model.Market;

/**
 * 运费计费策略。三个市场的数学结构互不相同，只能按实现隔离。
 */
public interface FreightRateStrategy {

    Market market();

    FreightQuote quote(ShipmentContext ctx, FreightRateSnapshot snapshot);
}