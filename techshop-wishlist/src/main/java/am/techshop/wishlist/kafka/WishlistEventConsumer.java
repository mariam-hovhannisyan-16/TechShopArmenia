package am.techshop.wishlist.kafka;

import am.techshop.common.event.UserDeletedEvent;
import am.techshop.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WishlistEventConsumer {

    private final WishlistService wishlistService;

    @KafkaListener(topics = "user-deleted", groupId = "wishlist-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserDeletedEvent"})
    public void handleUserDeleted(UserDeletedEvent event) {
        wishlistService.deleteWishlistForUser(event.userId());
    }
}
