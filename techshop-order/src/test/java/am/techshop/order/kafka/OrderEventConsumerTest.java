package am.techshop.order.kafka;

import am.techshop.common.event.UserDeletedEvent;
import am.techshop.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderEventConsumer consumer;

    @Test
    void handleUserDeleted_AnonymizesOrdersForUser() {
        UserDeletedEvent event = new UserDeletedEvent(1L);

        consumer.handleUserDeleted(event);

        verify(orderService).anonymizeOrdersForUser(1L);
    }
}
