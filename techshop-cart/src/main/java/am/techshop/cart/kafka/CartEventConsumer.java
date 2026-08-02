package am.techshop.cart.kafka;

import am.techshop.cart.service.CartService;
import am.techshop.common.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CartEventConsumer {

    private final CartService cartService;

    @KafkaListener(topics = "user-deleted", groupId = "cart-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserDeletedEvent"})
    public void handleUserDeleted(UserDeletedEvent event) {
        cartService.deleteCartForUser(event.userId());
    }
}
