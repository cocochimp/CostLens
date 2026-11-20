package com.costlens.domain.rule.model;

import com.costlens.common.exception.RuleNotFoundException;
import com.costlens.common.exception.RuleVersionConflictException;
import com.costlens.model.Carrier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 运费费率卡聚合根（写模式）。与 FeeRule 同构：版本只追加，因此历史运价可复现。
 */
public class FreightRateCard {

    private final String rateKey;
    private final Carrier carrier;
    private final List<FreightRateVersion> versions;

    public FreightRateCard(String rateKey, Carrier carrier) {
        this(rateKey, carrier, new ArrayList<>());
    }

    public FreightRateCard(String rateKey, Carrier carrier, List<FreightRateVersion> versions) {
        if (rateKey == null || rateKey.isBlank()) {
            throw new IllegalArgumentException("rateKey is blank");
        }
        if (carrier == null) {
            throw new IllegalArgumentException("carrier is null");
        }
        this.rateKey = rateKey;
        this.carrier = carrier;
        this.versions = new ArrayList<>(versions == null ? List.of() : versions);
    }

    public String rateKey() {
        return rateKey;
    }

    public Carrier carrier() {
        return carrier;
    }

    public List<FreightRateVersion> versions() {
        return List.copyOf(versions);
    }

    public FreightRateVersion resolveAt(LocalDateTime bizTime) {
        return versions.stream()
                .filter(v -> v.ref().validAt(bizTime))
                .max(Comparator.comparingInt(v -> v.ref().versionSeq()))
                .orElseThrow(() -> new RuleNotFoundException(rateKey, bizTime));
    }

    public FreightRateVersion resolveAsOf(LocalDateTime bizTime, LocalDateTime knownAt) {
        return versions.stream()
                .filter(v -> v.ref().knownAt(knownAt))
                .filter(v -> v.ref().validAt(bizTime))
                .max(Comparator.comparing(v -> v.ref().recordedAt()))
                .orElseThrow(() -> new RuleNotFoundException(
                        "rateKey=" + rateKey + ", bizTime=" + bizTime + ", knownAt=" + knownAt));
    }

    public FreightRateVersion appendVersion(FreightRateSnapshot snapshot,
                                            LocalDateTime validFrom,
                                            LocalDateTime validTo,
                                            LocalDateTime recordedAt) {
        LocalDateTime maxRecorded = versions.stream()
                .map(v -> v.ref().recordedAt())
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
        if (!recordedAt.isAfter(maxRecorded)) {
            throw new RuleVersionConflictException(rateKey, recordedAt, maxRecorded);
        }

        RuleVersionRef ref = new RuleVersionRef(
                0L, rateKey, versions.size() + 1, validFrom, validTo, recordedAt);
        FreightRateVersion version = new FreightRateVersion(ref, snapshot);
        versions.add(version);
        return version;
    }
}