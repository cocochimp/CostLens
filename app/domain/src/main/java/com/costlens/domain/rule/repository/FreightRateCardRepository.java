package com.costlens.domain.rule.repository;

import com.costlens.domain.rule.model.FreightRateCard;

import java.util.List;
import java.util.Optional;

/**
 * 运费费率卡仓储接口。
 */
public interface FreightRateCardRepository {

    Optional<FreightRateCard> findByKey(String rateKey);

    List<FreightRateCard> findAll();

    void save(FreightRateCard card);
}