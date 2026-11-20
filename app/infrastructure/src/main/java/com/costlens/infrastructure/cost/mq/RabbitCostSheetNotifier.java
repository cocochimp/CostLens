package com.costlens.infrastructure.cost.mq;

import com.costlens.application.cost.port.CostSheetNotifier;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 成本单确认事件发布。核心链路不依赖消息成功，因此发送失败只记日志不外抛。
 */
@Component
public class RabbitCostSheetNotifier implements CostSheetNotifier {

    private final RabbitTemplate rabbitTemplate;

    public RabbitCostSheetNotifier(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void notifyConfirmed(String sheetId, String orderNo, long totalCostMinor, String currency) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sheetId", sheetId);
        payload.put("orderNo", orderNo);
        payload.put("totalCostMinor", totalCostMinor);
        payload.put("currency", currency);
        rabbitTemplate.convertAndSend(RabbitTopologyConfig.EXCHANGE,
                RabbitTopologyConfig.ROUTING_KEY, payload);
    }
}