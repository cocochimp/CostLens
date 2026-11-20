package com.costlens.application.cost.port;

/**
 * 成本单事件通知端口。成本单确认后广播，供对账、报表等下游消费。
 */
public interface CostSheetNotifier {

    void notifyConfirmed(String sheetId, String orderNo, long totalCostMinor, String currency);
}