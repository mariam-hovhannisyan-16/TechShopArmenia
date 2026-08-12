package am.techshop.notification.kafka;

import am.techshop.common.event.AdminUserRegisteredEvent;
import am.techshop.common.event.ChatReplyEvent;
import am.techshop.common.event.OrderStatusChangedEvent;
import am.techshop.common.event.PasswordResetRequestedEvent;
import am.techshop.common.event.PriceDropEvent;
import am.techshop.common.event.UserDeletedEvent;
import am.techshop.common.event.UserRegisteredEvent;
import am.techshop.common.event.UserVerifiedEvent;
import am.techshop.notification.dispatch.NotificationDispatcher;
import am.techshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationDispatcher dispatcher;
    private final NotificationService notificationService;

    @KafkaListener(topics = "order-status-changed", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.OrderStatusChangedEvent"})
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        dispatcher.dispatchOrderStatusChanged(event.userId(), event.userEmail(), event.userName(),
                event.orderId(), event.status(), event.note(), event.totalPrice(),
                event.paymentMethod(), event.installmentPlan(), event.language(), event.productNames());
    }

    @KafkaListener(topics = "user-registered", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserRegisteredEvent"})
    public void handleUserRegistered(UserRegisteredEvent event) {
        dispatcher.dispatchEmailVerification(event.userId(), event.email(), event.name(), event.verificationToken());
    }

    @KafkaListener(topics = "user-verified", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserVerifiedEvent"})
    public void handleUserVerified(UserVerifiedEvent event) {
        dispatcher.dispatchWelcome(event.userId(), event.email(), event.name());
    }

    @KafkaListener(topics = "password-reset-requested", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.PasswordResetRequestedEvent"})
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        dispatcher.dispatchPasswordReset(event.userId(), event.email(), event.name(), event.resetToken());
    }

    @KafkaListener(topics = "chat-reply", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.ChatReplyEvent"})
    public void handleChatReply(ChatReplyEvent event) {
        dispatcher.dispatchChatReply(event.userId(), event.messagePreview());
    }

    @KafkaListener(topics = "price-drop", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.PriceDropEvent"})
    public void handlePriceDrop(PriceDropEvent event) {
        dispatcher.dispatchPriceDrop(event.userId(), event.productName(), event.newPrice());
    }

    @KafkaListener(topics = "admin-user-registered", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.AdminUserRegisteredEvent"})
    public void handleAdminUserRegistered(AdminUserRegisteredEvent event) {
        dispatcher.dispatchAdminNewUser(event.adminIds(), event.newUserName(), event.newUserEmail());
    }

    @KafkaListener(topics = "user-deleted", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserDeletedEvent"})
    public void handleUserDeleted(UserDeletedEvent event) {
        notificationService.deleteNotificationsForUser(event.userId());
    }
}
