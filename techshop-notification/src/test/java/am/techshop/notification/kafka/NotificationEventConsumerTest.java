package am.techshop.notification.kafka;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.event.ChatReplyEvent;
import am.techshop.common.event.OrderStatusChangedEvent;
import am.techshop.common.event.PasswordResetRequestedEvent;
import am.techshop.common.event.PriceDropEvent;
import am.techshop.common.event.UserDeletedEvent;
import am.techshop.common.event.UserRegisteredEvent;
import am.techshop.common.event.UserVerifiedEvent;
import am.techshop.notification.dispatch.NotificationDispatcher;
import am.techshop.notification.service.NotificationService;
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

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationEventConsumer consumer;

    @Test
    void handleOrderStatusChanged_DelegatesToDispatcher() {
        InstallmentPlanResponse installmentPlan = new InstallmentPlanResponse(
                "Ameriabank", BigDecimal.valueOf(0.12), 12, BigDecimal.valueOf(20), BigDecimal.valueOf(16.80));
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                42L, 1L, "mariam@test.com", "Mariam", OrderStatus.PAID, "Payment verified", BigDecimal.valueOf(200),
                PaymentMethod.INSTALLMENT, installmentPlan, Language.RU);

        consumer.handleOrderStatusChanged(event);

        verify(dispatcher).dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.PAID, "Payment verified", BigDecimal.valueOf(200), PaymentMethod.INSTALLMENT, installmentPlan, Language.RU);
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

    @Test
    void handleChatReply_DelegatesToDispatcher() {
        ChatReplyEvent event = new ChatReplyEvent(1L, 42L, "It shipped yesterday!");

        consumer.handleChatReply(event);

        verify(dispatcher).dispatchChatReply(1L, "It shipped yesterday!");
    }

    @Test
    void handlePriceDrop_DelegatesToDispatcher() {
        PriceDropEvent event = new PriceDropEvent(1L, 7L, "Phone", BigDecimal.valueOf(150));

        consumer.handlePriceDrop(event);

        verify(dispatcher).dispatchPriceDrop(1L, "Phone", BigDecimal.valueOf(150));
    }

    @Test
    void handleUserDeleted_DeletesNotificationsForUser() {
        UserDeletedEvent event = new UserDeletedEvent(1L);

        consumer.handleUserDeleted(event);

        verify(notificationService).deleteNotificationsForUser(1L);
    }
}
