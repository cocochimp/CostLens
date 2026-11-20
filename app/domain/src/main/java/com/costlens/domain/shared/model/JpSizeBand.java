package com.costlens.domain.shared.model;

import com.costlens.common.exception.OversizeException;

/**
 * ヤマト宅急便尺寸档。按三边和判定（含上界），重量只作上限校验，不参与计价。
 */
public enum JpSizeBand {

    S60(60, 2_000L),
    S80(80, 5_000L),
    S100(100, 10_000L),
    S120(120, 15_000L),
    S140(140, 20_000L),
    S160(160, 25_000L),
    S180(180, 30_000L);

    private final int girthCmMax;
    private final long weightCapGrams;

    JpSizeBand(int girthCmMax, long weightCapGrams) {
        this.girthCmMax = girthCmMax;
        this.weightCapGrams = weightCapGrams;
    }

    public int girthCmMax() {
        return girthCmMax;
    }

    public long weightCapGrams() {
        return weightCapGrams;
    }

    /** 按三边和落档，含上界：girthCm <= 档位上界即命中 */
    public static JpSizeBand ofGirth(int girthCm) {
        for (JpSizeBand band : values()) {
            if (girthCm <= band.girthCmMax) {
                return band;
            }
        }
        throw new OversizeException(girthCm);
    }

    /** 超重则升档，不按重量计价 */
    public JpSizeBand upgradeFor(Weight actual) {
        for (JpSizeBand band : values()) {
            if (band.girthCmMax >= this.girthCmMax && actual.grams() <= band.weightCapGrams) {
                return band;
            }
        }
        throw new OversizeException(girthCmMax);
    }
}