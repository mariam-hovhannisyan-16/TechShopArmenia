package am.techshop.notification.dispatch;

import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.notification.entity.Notification;
import am.techshop.notification.mapper.NotificationMapper;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationDispatcher dispatcher;

    private void stubNotificationCreation() {
        when(notificationMapper.toEntity(any(), any())).thenAnswer(inv -> {
            Notification notification = new Notification();
            notification.setUserId(inv.getArgument(0));
            notification.setMessage(inv.getArgument(1));
            return notification;
        });
    }

    @Test
    void dispatchEmailVerification_SendsBothEmailAndInAppReminder() {
        stubNotificationCreation();

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
        stubNotificationCreation();

        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.PAID, "Payment verified", BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.EN, null);

        verify(emailService).sendOrderStatusChanged(
                "mariam@test.com", "Mariam", 42L, OrderStatus.PAID, BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.EN, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("42"));
        assertTrue(captor.getValue().getMessage().contains("Paid"));
    }

    @Test
    void dispatchOrderStatusChanged_WhenArmenian_WritesInAppMessageInArmenian() {
        stubNotificationCreation();

        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.PAID, "Payment verified via IDRAM", BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.HY, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        String message = captor.getValue().getMessage();
        assertTrue(message.contains("Ձեր՝ #42 պատվերի կարգավիճակը փոխվել է"));
        assertTrue(message.contains("Վճարված"));
        assertTrue(message.contains("Նշում"));
        assertTrue(message.contains("Idram"));
        assertFalse(message.contains("has been created"), "Armenian message should not contain leftover English copy");
    }

    @Test
    void dispatchOrderStatusChanged_WhenRussian_WritesInAppMessageInRussian() {
        stubNotificationCreation();

        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.SHIPPED, null, BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.RU, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        String message = captor.getValue().getMessage();
        assertEquals("Статус вашего заказа №42 изменён на: Отправлен.", message);
    }

    @Test
    void dispatchOrderStatusChanged_WhenSingleProduct_IncludesProductNameInArmenianMessage() {
        stubNotificationCreation();

        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.DELIVERED, null, BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.HY,
                List.of("iPhone 15, 128GB"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("Ձեր՝ #42 պատվերի (iPhone 15, 128GB) կարգավիճակը փոխվել է. Հասցվել է:", captor.getValue().getMessage());
    }

    @Test
    void dispatchOrderStatusChanged_WhenManyProducts_TruncatesToFirstPlusCountInEnglishMessage() {
        stubNotificationCreation();

        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.SHIPPED, null, BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.EN,
                List.of("iPhone 15, 128GB", "AirPods Pro", "USB-C Cable"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("Your order #42 (iPhone 15, 128GB and 2 more) status changed to Shipped.", captor.getValue().getMessage());
    }

    @Test
    void dispatchOrderStatusChanged_WhenNoProducts_OmitsParensFromMessage() {
        stubNotificationCreation();

        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.SHIPPED, null, BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.EN, List.of());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals("Your order #42 status changed to Shipped.", captor.getValue().getMessage());
    }

    @Test
    void dispatchChatReply_SavesInAppNotificationOnly() {
        stubNotificationCreation();

        dispatcher.dispatchChatReply(1L, "We can help with that!");

        verify(emailService, never()).sendVerificationEmail(any(), any(), any());
        verify(emailService, never()).sendWelcome(any(), any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
        verify(emailService, never()).sendOrderStatusChanged(any(), any(), any(), any(), any(), any(), any(), any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("We can help with that!"));
        assertEquals(1L, captor.getValue().getUserId());
    }

    @Test
    void dispatchPriceDrop_SavesInAppNotificationOnly() {
        stubNotificationCreation();

        dispatcher.dispatchPriceDrop(1L, "iPhone 15", BigDecimal.valueOf(400000));

        verify(emailService, never()).sendOrderStatusChanged(any(), any(), any(), any(), any(), any(), any(), any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("iPhone 15"));
        assertTrue(captor.getValue().getMessage().contains("400000"));
    }

    @Test
    void dispatchAdminNewUser_SavesTrilingualInAppNotificationPerAdmin() {
        stubNotificationCreation();

        dispatcher.dispatchAdminNewUser(List.of(1L, 2L), "Mariam", "mariam@test.com");

        verify(emailService, never()).sendWelcome(any(), any());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertEquals(List.of(1L, 2L), saved.stream().map(Notification::getUserId).toList());
        for (Notification notification : saved) {
            String message = notification.getMessage();
            assertTrue(message.contains("Mariam"));
            assertTrue(message.contains("mariam@test.com"));
            assertTrue(message.contains("Նոր օգտատեր գրանցվել է"));
            assertTrue(message.contains("New user registered"));
            assertTrue(message.contains("Зарегистрировался новый пользователь"));
        }
    }

    @Test
    void dispatchOrderStatusChanged_WhenPending_UsesCreatedMessage() {
        stubNotificationCreation();

        dispatcher.dispatchOrderStatusChanged(1L, "mariam@test.com", "Mariam", 42L,
                OrderStatus.PENDING, "Order created from cart", BigDecimal.valueOf(200), PaymentMethod.IDRAM, null, Language.EN, null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getMessage().contains("has been created"));
    }
}
