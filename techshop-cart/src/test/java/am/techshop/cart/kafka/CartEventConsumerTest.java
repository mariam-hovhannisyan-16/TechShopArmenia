package am.techshop.cart.kafka;

import am.techshop.cart.service.CartService;
import am.techshop.common.event.UserDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartEventConsumerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartEventConsumer consumer;

    @Test
    void handleUserDeleted_DeletesCartForUser() {
        UserDeletedEvent event = new UserDeletedEvent(1L);

        consumer.handleUserDeleted(event);

        verify(cartService).deleteCartForUser(1L);
    }
}
