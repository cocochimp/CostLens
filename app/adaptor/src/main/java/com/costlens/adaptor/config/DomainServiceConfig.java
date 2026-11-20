package com.costlens.adaptor.config;

import com.costlens.domain.fee.service.FeeChargeService;
import com.costlens.domain.freight.service.FreightRatingService;
import com.costlens.domain.freight.service.SurchargeCalculator;
import com.costlens.domain.freight.service.strategy.DimWeightZoneStrategy;
import com.costlens.domain.freight.service.strategy.FirstAddWeightStrategy;
import com.costlens.domain.freight.service.strategy.GirthSizeBandStrategy;
import com.costlens.domain.pricing.service.PricingSolver;
import com.costlens.domain.rule.service.BitTemporalResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 领域服务装配。domain 层零框架依赖，Bean 在此显式注册。
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public SurchargeCalculator surchargeCalculator() {
        // 默认无附加费；接入燃油周表后替换为真实实现
        return SurchargeCalculator.NONE;
    }

    @Bean
    public FreightRatingService freightRatingService(SurchargeCalculator surchargeCalculator) {
        return new FreightRatingService(List.of(
                new DimWeightZoneStrategy(surchargeCalculator),
                new GirthSizeBandStrategy(surchargeCalculator),
                new FirstAddWeightStrategy(surchargeCalculator)));
    }

    @Bean
    public FeeChargeService feeChargeService() {
        return new FeeChargeService();
    }

    @Bean
    public PricingSolver pricingSolver() {
        return new PricingSolver();
    }

    @Bean
    public BitTemporalResolver bitTemporalResolver() {
        return new BitTemporalResolver();
    }
}