package com.costlens.domain.cost.model;

import com.costlens.common.exception.CostLensException;
import com.costlens.common.exception.ErrorCodes;
import com.costlens.domain.fee.model.CostItem;
import com.costlens.domain.shared.model.Money;
import com.costlens.domain.shared.model.Rate;
import com.costlens.model.CostSheetStatus;
import com.costlens.model.Platform;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单成本单聚合根（写模式）。
 * 状态机：DRAFT → CONFIRMED → SUPERSEDED；DRAFT → SUPERSEDED。
 * 已确认的单不允许回退，只能被重算产出的新单取代。
 */
public class OrderCostSheet {

    private final String sheetId;
    private final String orderNo;
    private final Platform platform;
    private final Money revenue;
    private final List<CostItem> items;
    private CostSheetStatus status;
    private String recalcBatchId;

    public OrderCostSheet(String sheetId, String orderNo, Platform platform, Money revenue) {
        this(sheetId, orderNo, platform, revenue, new ArrayList<>(), CostSheetStatus.DRAFT, null);
    }

    public OrderCostSheet(String sheetId, String orderNo, Platform platform, Money revenue,
                          List<CostItem> items, CostSheetStatus status, String recalcBatchId) {
        if (sheetId == null || sheetId.isBlank()) {
            throw new IllegalArgumentException("sheetId is blank");
        }
        if (orderNo == null || orderNo.isBlank()) {
            throw new IllegalArgumentException("orderNo is blank");
        }
        if (platform == null || revenue == null) {
            throw new IllegalArgumentException("platform and revenue are required");
        }
        if (revenue.isNegative()) {
            throw new IllegalArgumentException("revenue must be >= 0");
        }
        this.sheetId = sheetId;
        this.orderNo = orderNo;
        this.platform = platform;
        this.revenue = revenue;
        this.items = new ArrayList<>(items == null ? List.of() : items);
        this.status = status == null ? CostSheetStatus.DRAFT : status;
        this.recalcBatchId = recalcBatchId;
    }

    public void addItem(CostItem item) {
        if (status != CostSheetStatus.DRAFT) {
            throw new IllegalStateException("只有 DRAFT 状态可追加费用项");
        }
        if (!item.amount().currency().equals(revenue.currency())) {
            throw new CostLensException(ErrorCodes.CURRENCY_MISMATCH,
                    "币种不一致，费用项与成本单不同: item=" + item.amount().currency()
                            + ", sheet=" + revenue.currency());
        }
        items.add(item);
    }

    public void confirm() {
        if (status != CostSheetStatus.DRAFT) {
            throw new IllegalStateException("只有 DRAFT 可确认，当前=" + status);
        }
        this.status = CostSheetStatus.CONFIRMED;
    }

    /** 被重算产出的新单取代；旧单保留，不可删除 */
    public void supersede(String byBatchId) {
        if (status == CostSheetStatus.SUPERSEDED) {
            throw new IllegalStateException("成本单已被取代，不可重复取代");
        }
        this.status = CostSheetStatus.SUPERSEDED;
        this.recalcBatchId = byBatchId;
    }

    public Money totalCost() {
        return items.stream()
                .map(CostItem::amount)
                .reduce(Money.zero(revenue.currency()), Money::plus);
    }

    public Money netProfit() {
        return revenue.minus(totalCost());
    }

    /** 净利率 = 净利 / 收入。收入为 0 时返回 0 */
    public Rate netMargin() {
        if (revenue.isZero()) {
            return Rate.zero();
        }
        BigDecimal margin = netProfit().toDecimal()
                .divide(revenue.toDecimal(), 8, RoundingMode.HALF_UP);
        return Rate.ofRatio(margin);
    }

    public String sheetId() {
        return sheetId;
    }

    public String orderNo() {
        return orderNo;
    }

    public Platform platform() {
        return platform;
    }

    public Money revenue() {
        return revenue;
    }

    public List<CostItem> items() {
        return List.copyOf(items);
    }

    public CostSheetStatus status() {
        return status;
    }

    public String recalcBatchId() {
        return recalcBatchId;
    }
}