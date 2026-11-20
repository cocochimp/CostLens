package com.costlens.infrastructure.cost.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 消息拓扑。队列与交换机均持久化，应用重启不丢消息。
 */
@Configuration
public class RabbitTopologyConfig {

    public static final String EXCHANGE = "costlens.topic";
    public static final String QUEUE_SHEET_CONFIRMED = "costlens.cost.sheet.confirmed";
    public static final String ROUTING_KEY = "cost.sheet.confirmed";

    @Bean
    public TopicExchange costLensExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue costSheetConfirmedQueue() {
        return QueueBuilder.durable(QUEUE_SHEET_CONFIRMED).build();
    }

    @Bean
    public Binding costSheetConfirmedBinding(Queue costSheetConfirmedQueue,
                                             TopicExchange costLensExchange) {
        return BindingBuilder.bind(costSheetConfirmedQueue)
                .to(costLensExchange)
                .with(ROUTING_KEY);
    }
}