package am.techshop.order.kafka;

import am.techshop.common.event.UserDeletedEvent;
import am.techshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "user-deleted", groupId = "order-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserDeletedEvent"})
    public void handleUserDeleted(UserDeletedEvent event) {
        orderService.anonymizeOrdersForUser(event.userId());
    }
}
