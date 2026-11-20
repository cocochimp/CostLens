package com.costlens.model;

/**
 * 成本单状态。已确认的单不允许回退，只能被取代。
 */
public enum CostSheetStatus {

    DRAFT,
    CONFIRMED,
    SUPERSEDED
}