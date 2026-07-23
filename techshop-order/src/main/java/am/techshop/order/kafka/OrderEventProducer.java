package am.techshop.order.kafka;

import am.techshop.common.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderStatusChangedEvent> kafkaTemplate;

    public void sendOrderStatusChangedEvent(OrderStatusChangedEvent event) {
        kafkaTemplate.send("order-status-changed", event);
    }
}
