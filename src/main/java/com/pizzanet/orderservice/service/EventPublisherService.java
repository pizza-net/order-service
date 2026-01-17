package com.pizzanet.orderservice.service;

import com.pizzanet.orderservice.config.RabbitMQConfig;
import com.pizzanet.orderservice.dto.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_STATUS_ROUTING_KEY,
                event
            );
            log.info("Published order status changed event for order #{}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to publish order status changed event for order #{}: {}", 
                     event.getOrderId(), e.getMessage(), e);
        }
    }
}
