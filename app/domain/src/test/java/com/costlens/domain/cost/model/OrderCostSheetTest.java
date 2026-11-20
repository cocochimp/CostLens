package com.costlens.domain.cost.model;

import com.costlens.domain.fee.model.CostItem;
import com.costlens.domain.shared.model.Money;
import com.costlens.model.ChargeRuleType;
import com.costlens.model.CostSheetStatus;
import com.costlens.model.CostType;
import com.costlens.model.Currency;
import com.costlens.model.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCostSheetTest {

    private static CostItem item(CostType type, long minor) {
        return new CostItem(type, "PERCENTAGE", "K", 1,
                Money.ofMinor(minor, Currency.USD), Map.of());
    }

    private OrderCostSheet sheet() {
        return new OrderCostSheet("S1", "ORD-1", Platform.AMAZON_US,
                Money.ofMinor(2_500, Currency.USD));
    }

    @Test
    @DisplayName("成本单汇总与净利率")
    void totalsAndMargin() {
        OrderCostSheet sheet = sheet();
        sheet.addItem(item(CostType.FREIGHT, 450));
        sheet.addItem(item(CostType.COMMISSION, 375));
        sheet.addItem(item(CostType.PAYMENT, 110));

        assertThat(sheet.totalCost().minorUnits()).isEqualTo(935L);
        assertThat(sheet.netProfit().minorUnits()).isEqualTo(1_565L);
        // 1565 / 2500 = 62.6%
        assertThat(sheet.netMargin().value().doubleValue()).isCloseTo(0.626, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    @DisplayName("状态机：DRAFT → CONFIRMED，已确认不可回退")
    void confirmIsTerminalForward() {
        OrderCostSheet sheet = sheet();
        sheet.addItem(item(CostType.FREIGHT, 100));
        sheet.confirm();

        assertThat(sheet.status()).isEqualTo(CostSheetStatus.CONFIRMED);
        assertThatThrownBy(sheet::confirm).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> sheet.addItem(item(CostType.PAYMENT, 10)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("重算取代：旧单保留并标记 SUPERSEDED")
    void supersedeKeepsOldSheet() {
        OrderCostSheet sheet = sheet();
        sheet.confirm();
        sheet.supersede("BATCH-1");

        assertThat(sheet.status()).isEqualTo(CostSheetStatus.SUPERSEDED);
        assertThat(sheet.recalcBatchId()).isEqualTo("BATCH-1");
        assertThatThrownBy(() -> sheet.supersede("BATCH-2")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("币种不一致拒绝入单，不做隐式换算")
    void currencyMismatchRejected() {
        OrderCostSheet sheet = sheet();
        CostItem jpy = new CostItem(CostType.FREIGHT, "PERCENTAGE", "K", 1,
                Money.ofMinor(100, Currency.JPY), Map.of());

        assertThatThrownBy(() -> sheet.addItem(jpy)).hasMessageContaining("币种不一致");
    }
}