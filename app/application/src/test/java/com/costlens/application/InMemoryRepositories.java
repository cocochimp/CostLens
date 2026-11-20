package com.costlens.application;

import com.costlens.domain.cost.model.OrderCostSheet;
import com.costlens.domain.cost.model.RecalcBatch;
import com.costlens.domain.cost.repository.CostSheetRepository;
import com.costlens.domain.cost.repository.RecalcBatchRepository;
import com.costlens.domain.rule.model.FeeRule;
import com.costlens.domain.rule.model.FreightRateCard;
import com.costlens.domain.rule.repository.FeeRuleRepository;
import com.costlens.domain.rule.repository.FreightRateCardRepository;
import com.costlens.model.Platform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 测试用内存仓储。接口与 infrastructure 实现一致，用于脱离数据库验证业务链路。
 */
public final class InMemoryRepositories {

    private InMemoryRepositories() {
    }

    public static class FeeRuleRepo implements FeeRuleRepository {
        private final Map<String, FeeRule> store = new LinkedHashMap<>();

        @Override
        public Optional<FeeRule> findByKey(String ruleKey) {
            return Optional.ofNullable(store.get(ruleKey));
        }

        @Override
        public List<FeeRule> findByPlatformAndCategory(Platform platform, String category) {
            return store.values().stream()
                    .filter(r -> r.platform() == platform)
                    .filter(r -> "ALL".equals(r.category()) || r.category().equals(category))
                    .toList();
        }

        @Override
        public void save(FeeRule rule) {
            store.put(rule.ruleKey(), rule);
        }
    }

    public static class FreightRateCardRepo implements FreightRateCardRepository {
        private final Map<String, FreightRateCard> store = new LinkedHashMap<>();

        @Override
        public Optional<FreightRateCard> findByKey(String rateKey) {
            return Optional.ofNullable(store.get(rateKey));
        }

        @Override
        public List<FreightRateCard> findAll() {
            return new ArrayList<>(store.values());
        }

        @Override
        public void save(FreightRateCard card) {
            store.put(card.rateKey(), card);
        }
    }

    public static class CostSheetRepo implements CostSheetRepository {
        private final Map<String, OrderCostSheet> store = new LinkedHashMap<>();

        @Override
        public Optional<OrderCostSheet> findById(String sheetId) {
            return Optional.ofNullable(store.get(sheetId));
        }

        @Override
        public Optional<OrderCostSheet> findByOrderNo(String orderNo, String recalcBatchId) {
            return store.values().stream()
                    .filter(s -> s.orderNo().equals(orderNo))
                    .filter(s -> recalcBatchId == null
                            ? s.recalcBatchId() == null
                            : recalcBatchId.equals(s.recalcBatchId()))
                    .findFirst();
        }

        @Override
        public void save(OrderCostSheet sheet) {
            store.put(sheet.sheetId(), sheet);
        }
    }

    public static class RecalcBatchRepo implements RecalcBatchRepository {
        private final Map<String, RecalcBatch> store = new LinkedHashMap<>();

        @Override
        public Optional<RecalcBatch> findById(String batchId) {
            return Optional.ofNullable(store.get(batchId));
        }

        @Override
        public void save(RecalcBatch batch) {
            store.put(batch.batchId(), batch);
        }
    }

    /** 内存幂等闸门：模拟 Redis 的 SETNX 语义 */
    public static class InMemoryIdempotencyGuard
            implements com.costlens.application.cost.port.IdempotencyGuard {
        private final java.util.Set<String> keys = new java.util.HashSet<>();

        @Override
        public boolean tryAcquire(String key, java.time.Duration ttl) {
            return keys.add(key);
        }
    }

    /** 记录事件的通知器，用于断言成本单确认后确实广播 */
    public static class RecordingNotifier
            implements com.costlens.application.cost.port.CostSheetNotifier {
        private final List<String> events = new ArrayList<>();

        @Override
        public void notifyConfirmed(String sheetId, String orderNo, long totalCostMinor, String currency) {
            events.add(sheetId + "|" + orderNo + "|" + totalCostMinor + "|" + currency);
        }

        public List<String> events() {
            return events;
        }
    }
}