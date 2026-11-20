package com.costlens.infrastructure.rule.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.costlens.domain.rule.model.FreightRateCard;
import com.costlens.domain.rule.model.FreightRateParams;
import com.costlens.domain.rule.model.FreightRateSnapshot;
import com.costlens.domain.rule.model.FreightRateVersion;
import com.costlens.domain.rule.model.JpSizeRate;
import com.costlens.domain.rule.model.RuleVersionRef;
import com.costlens.domain.rule.model.SeaFirstAddRate;
import com.costlens.domain.rule.model.UsZoneRate;
import com.costlens.domain.rule.model.ZoneRange;
import com.costlens.domain.rule.repository.FreightRateCardRepository;
import com.costlens.domain.shared.model.JpSizeBand;
import com.costlens.domain.shared.model.Money;
import com.costlens.infrastructure.rule.mysql.FreightRateVersionMapper;
import com.costlens.infrastructure.rule.mysql.JpSizeRateMapper;
import com.costlens.infrastructure.rule.mysql.SeaFirstAddRateMapper;
import com.costlens.infrastructure.rule.mysql.UsZoneRateMapper;
import com.costlens.infrastructure.rule.mysql.ZoneMapMapper;
import com.costlens.infrastructure.rule.mysql.po.FreightRateVersionPO;
import com.costlens.infrastructure.rule.mysql.po.JpSizeRatePO;
import com.costlens.infrastructure.rule.mysql.po.SeaFirstAddRatePO;
import com.costlens.infrastructure.rule.mysql.po.UsZoneRatePO;
import com.costlens.infrastructure.rule.mysql.po.ZoneMapPO;
import com.costlens.infrastructure.shared.JsonCodec;
import com.costlens.model.Carrier;
import com.costlens.model.Currency;
import com.costlens.model.Market;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 运费费率卡仓储实现。版本与三张子费率表一起装配成快照。
 */
@Repository
public class FreightRateCardRepositoryImpl implements FreightRateCardRepository {

    private final FreightRateVersionMapper versionMapper;
    private final UsZoneRateMapper usZoneRateMapper;
    private final ZoneMapMapper zoneMapMapper;
    private final JpSizeRateMapper jpSizeRateMapper;
    private final SeaFirstAddRateMapper seaFirstAddRateMapper;
    private final JsonCodec codec;

    public FreightRateCardRepositoryImpl(FreightRateVersionMapper versionMapper,
                                         UsZoneRateMapper usZoneRateMapper,
                                         ZoneMapMapper zoneMapMapper,
                                         JpSizeRateMapper jpSizeRateMapper,
                                         SeaFirstAddRateMapper seaFirstAddRateMapper,
                                         JsonCodec codec) {
        this.versionMapper = versionMapper;
        this.usZoneRateMapper = usZoneRateMapper;
        this.zoneMapMapper = zoneMapMapper;
        this.jpSizeRateMapper = jpSizeRateMapper;
        this.seaFirstAddRateMapper = seaFirstAddRateMapper;
        this.codec = codec;
    }

    @Override
    public Optional<FreightRateCard> findByKey(String rateKey) {
        List<FreightRateVersionPO> rows = versionMapper.selectList(
                Wrappers.<FreightRateVersionPO>lambdaQuery()
                        .eq(FreightRateVersionPO::getRateKey, rateKey)
                        .orderByAsc(FreightRateVersionPO::getVersionSeq));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Carrier carrier = Carrier.valueOf(rows.get(0).getCarrier());
        List<FreightRateVersion> versions = rows.stream().map(this::toVersion).toList();
        return Optional.of(new FreightRateCard(rateKey, carrier, versions));
    }

    @Override
    public List<FreightRateCard> findAll() {
        List<FreightRateVersionPO> rows = versionMapper.selectList(
                Wrappers.<FreightRateVersionPO>lambdaQuery()
                        .orderByAsc(FreightRateVersionPO::getRateKey)
                        .orderByAsc(FreightRateVersionPO::getVersionSeq));
        return rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(FreightRateVersionPO::getRateKey))
                .entrySet().stream()
                .map(e -> new FreightRateCard(e.getKey(),
                        Carrier.valueOf(e.getValue().get(0).getCarrier()),
                        e.getValue().stream().map(this::toVersion).toList()))
                .toList();
    }

    @Override
    public void save(FreightRateCard card) {
        for (FreightRateVersion version : card.versions()) {
            Long existing = versionMapper.selectCount(Wrappers.<FreightRateVersionPO>lambdaQuery()
                    .eq(FreightRateVersionPO::getRateKey, card.rateKey())
                    .eq(FreightRateVersionPO::getVersionSeq, version.ref().versionSeq()));
            if (existing != null && existing > 0) {
                continue;
            }
            FreightRateVersionPO po = toPO(card, version);
            versionMapper.insert(po);
            insertChildren(po.getId(), version.snapshot());
        }
    }

    private FreightRateVersion toVersion(FreightRateVersionPO po) {
        FreightRateParams params = codec.read(po.getParamsJson(), FreightRateParams.class);
        Currency currency = Currency.of(po.getCurrency());
        Market market = Market.valueOf(po.getMarket());

        List<UsZoneRate> usRates = usZoneRateMapper.selectList(
                Wrappers.<UsZoneRatePO>lambdaQuery().eq(UsZoneRatePO::getRateVersionId, po.getId()))
                .stream()
                .map(r -> new UsZoneRate(r.getZone(), r.getServiceLevel(),
                        r.getWeightFromGram(), r.getWeightToGram(),
                        Money.ofMinor(r.getUnitPriceMinor(), Currency.of(r.getCurrency()))))
                .toList();

        List<ZoneRange> zoneRanges = zoneMapMapper.selectList(
                Wrappers.<ZoneMapPO>lambdaQuery().eq(ZoneMapPO::getRateVersionId, po.getId()))
                .stream()
                .map(r -> new ZoneRange(r.getZipPrefixFrom(), r.getZipPrefixTo(), r.getZone()))
                .toList();

        List<JpSizeRate> jpRates = jpSizeRateMapper.selectList(
                Wrappers.<JpSizeRatePO>lambdaQuery().eq(JpSizeRatePO::getRateVersionId, po.getId()))
                .stream()
                .map(r -> new JpSizeRate(r.getArea(), JpSizeBand.valueOf(r.getSizeBand()),
                        Money.ofMinor(r.getPriceMinor(), Currency.of(r.getCurrency()))))
                .toList();

        List<SeaFirstAddRate> seaRates = seaFirstAddRateMapper.selectList(
                Wrappers.<SeaFirstAddRatePO>lambdaQuery().eq(SeaFirstAddRatePO::getRateVersionId, po.getId()))
                .stream()
                .map(r -> new SeaFirstAddRate(r.getCountry(),
                        r.getFirstWeightGram(),
                        Money.ofMinor(r.getFirstPriceMinor(), Currency.of(r.getCurrency())),
                        r.getContinuationUnitGram(),
                        Money.ofMinor(r.getContinuationPriceMinor(), Currency.of(r.getCurrency()))))
                .toList();

        FreightRateSnapshot snapshot = new FreightRateSnapshot(
                po.getRateKey(), po.getVersionSeq(), market, currency,
                params.dimDivisor(), params.roundStepGrams(),
                usRates, zoneRanges, jpRates, seaRates);

        RuleVersionRef ref = new RuleVersionRef(po.getId(), po.getRateKey(), po.getVersionSeq(),
                po.getValidFrom(), po.getValidTo(), po.getRecordedAt());
        return new FreightRateVersion(ref, snapshot);
    }

    private FreightRateVersionPO toPO(FreightRateCard card, FreightRateVersion version) {
        FreightRateSnapshot snapshot = version.snapshot();
        FreightRateVersionPO po = new FreightRateVersionPO();
        po.setRateKey(card.rateKey());
        po.setVersionSeq(version.ref().versionSeq());
        po.setCarrier(card.carrier().name());
        po.setMarket(snapshot.market().name());
        po.setCurrency(snapshot.currency().name());
        po.setParamsJson(codec.write(new FreightRateParams(snapshot.dimDivisor(), snapshot.roundStepGrams())));
        po.setValidFrom(version.ref().validFrom());
        po.setValidTo(version.ref().validTo());
        po.setRecordedAt(version.ref().recordedAt());
        return po;
    }

    private void insertChildren(Long rateVersionId, FreightRateSnapshot snapshot) {
        for (UsZoneRate rate : snapshot.usZoneRates()) {
            UsZoneRatePO po = new UsZoneRatePO();
            po.setRateVersionId(rateVersionId);
            po.setZone(rate.zone());
            po.setServiceLevel(rate.serviceLevel());
            po.setWeightFromGram(rate.weightFromGram());
            po.setWeightToGram(rate.weightToGram());
            po.setUnitPriceMinor(rate.unitPricePerKg().minorUnits());
            po.setCurrency(rate.unitPricePerKg().currency().name());
            usZoneRateMapper.insert(po);
        }
        for (ZoneRange range : snapshot.zoneRanges()) {
            ZoneMapPO po = new ZoneMapPO();
            po.setRateVersionId(rateVersionId);
            po.setZipPrefixFrom(range.zipPrefixFrom());
            po.setZipPrefixTo(range.zipPrefixTo());
            po.setZone(range.zone());
            zoneMapMapper.insert(po);
        }
        for (JpSizeRate rate : snapshot.jpSizeRates()) {
            JpSizeRatePO po = new JpSizeRatePO();
            po.setRateVersionId(rateVersionId);
            po.setArea(rate.area());
            po.setSizeBand(rate.sizeBand().name());
            po.setGirthCmMax(rate.sizeBand().girthCmMax());
            po.setWeightCapGram(rate.sizeBand().weightCapGrams());
            po.setPriceMinor(rate.price().minorUnits());
            po.setCurrency(rate.price().currency().name());
            jpSizeRateMapper.insert(po);
        }
        for (SeaFirstAddRate rate : snapshot.seaFirstAddRates()) {
            SeaFirstAddRatePO po = new SeaFirstAddRatePO();
            po.setRateVersionId(rateVersionId);
            po.setCountry(rate.country());
            po.setFirstWeightGram(rate.firstWeightGram());
            po.setFirstPriceMinor(rate.firstPrice().minorUnits());
            po.setContinuationUnitGram(rate.continuationUnitGram());
            po.setContinuationPriceMinor(rate.continuationPrice().minorUnits());
            po.setCurrency(rate.firstPrice().currency().name());
            seaFirstAddRateMapper.insert(po);
        }
    }
}