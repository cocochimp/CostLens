package com.costlens.domain.freight.service;

import com.costlens.domain.freight.model.ShipmentContext;
import com.costlens.domain.shared.model.Money;

/**
 * 附加费计算。燃油、住宅、偏远、離島、クール便均由此扩展；默认无附加费。
 */
@FunctionalInterface
public interface SurchargeCalculator {

    SurchargeCalculator NONE = (ctx, base) -> Money.zero(base.currency());

    Money surcharge(ShipmentContext ctx, Money baseFreight);
}