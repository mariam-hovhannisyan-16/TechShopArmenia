package am.techshop.notification.dispatch;

import am.techshop.common.enums.OrderStatus;
import am.techshop.notification.entity.Notification;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    @Test
    void dispatchEmailVerification_SendsBothEmailAndInAppReminder() {
        dispatcher.dispatchEmailVerification(1L, "mariam@test.com", "Mariam", "token-123");

        verify(emailService).sendVerificationEmail("mariam@test.com", "Mariam", "token-123");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertTrue(captor.getValue().getMessage().contains("Mariam"));
        assertTrue(captor.getValue().getMessage().contains("հաստատել ձեր էլ. հասցեն"));
    }

    @Test
    void dispatchWelcome_SendsEmailOnly() {
        dispatcher.dispatchWelcome(1L, "mariam@test.com", "Mariam");

        verify(emailService).sendWelcome("mariam@test.com", "Mariam");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void dispatchPasswordReset_SendsEmailOnly() {
        dispatcher.dispatchPasswordReset(1L, "mariam@test.com", "Mariam", "reset-token");

        verify(emailService).sendPasswordResetEmail("mariam@test.com", "Mariam", "reset-token");
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void dispatchOrderStatusChanged_SendsBothEmailAndInApp() {
        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.PAID, "Payment verified", BigDecimal.valueOf(200));

        verify(emailService).sendOrderStatusChanged("mariam@test.com", "Mariam", 42L, OrderStatus.PAID, BigDecimal.valueOf(200));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("42"));
        assertTrue(captor.getValue().getMessage().contains("PAID"));
    }

    @Test
    void dispatchChatReply_SavesInAppNotificationOnly() {
        dispatcher.dispatchChatReply(1L, 7L, "We can help with that!");

        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        verify(emailService, never()).sendWelcome(any(), any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
        verify(emailService, never()).sendOrderStatusChanged(any(), any(), any(), any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("We can help with that!"));
        assertTrue(captor.getValue().getUserId().equals(1L));
    }

    @Test
    void dispatchPriceDrop_SavesInAppNotificationOnly() {
        dispatcher.dispatchPriceDrop(1L, 5L, "iPhone 15", BigDecimal.valueOf(400000));

        verify(emailService, never()).sendOrderStatusChanged(any(), any(), any(), any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("iPhone 15"));
        assertTrue(captor.getValue().getMessage().contains("400000"));
    }

    @Test
    void dispatchOrderStatusChanged_WhenPending_UsesCreatedMessage() {
        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.PENDING, "Order created from cart", BigDecimal.valueOf(200));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("has been created"));
    }
}
