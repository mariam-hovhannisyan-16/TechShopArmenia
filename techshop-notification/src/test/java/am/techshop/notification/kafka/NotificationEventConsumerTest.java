package am.techshop.notification.kafka;

import am.techshop.common.enums.OrderStatus;
import am.techshop.common.event.OrderStatusChangedEvent;
import am.techshop.common.event.PasswordResetRequestedEvent;
import am.techshop.common.event.UserRegisteredEvent;
import am.techshop.common.event.UserVerifiedEvent;
import am.techshop.notification.dispatch.NotificationDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventConsumerTest {

    @Mock
    private NotificationDispatcher dispatcher;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    void handleOrderStatusChanged_DelegatesToDispatcher() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                42L, 1L, "mariam@test.com", "Mariam", OrderStatus.PAID, "Payment verified", BigDecimal.valueOf(200));

        consumer.handleOrderStatusChanged(event);

        verify(dispatcher).dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.PAID, "Payment verified", BigDecimal.valueOf(200));
    }

    @Test
    void handleUserRegistered_DelegatesToDispatcher() {
        UserRegisteredEvent event = new UserRegisteredEvent(1L, "mariam@test.com", "Mariam", "verify-token");

        consumer.handleUserRegistered(event);

        verify(dispatcher).dispatchEmailVerification(1L, "mariam@test.com", "Mariam", "verify-token");
    }

    @Test
    void handleUserVerified_DelegatesToDispatcher() {
        UserVerifiedEvent event = new UserVerifiedEvent(1L, "mariam@test.com", "Mariam");

        consumer.handleUserVerified(event);

        verify(dispatcher).dispatchWelcome(1L, "mariam@test.com", "Mariam");
    }

    @Test
    void handlePasswordResetRequested_DelegatesToDispatcher() {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent(1L, "mariam@test.com", "Mariam", "reset-token");

        consumer.handlePasswordResetRequested(event);

        verify(dispatcher).dispatchPasswordReset(1L, "mariam@test.com", "Mariam", "reset-token");
    }
}
