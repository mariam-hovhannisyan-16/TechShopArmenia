package am.techshop.wishlist.kafka;

import am.techshop.common.event.UserDeletedEvent;
import am.techshop.wishlist.service.WishlistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WishlistEventConsumerTest {

    @Mock
    private WishlistService wishlistService;

    @InjectMocks
    private WishlistEventConsumer consumer;

    @Test
    void handleUserDeleted_DeletesWishlistForUser() {
        UserDeletedEvent event = new UserDeletedEvent(1L);

        consumer.handleUserDeleted(event);

        verify(wishlistService).deleteWishlistForUser(1L);
    }
}
