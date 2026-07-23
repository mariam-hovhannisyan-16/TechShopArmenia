package am.techshop.notification.dispatch;

import am.techshop.common.enums.OrderStatus;
import am.techshop.notification.entity.Notification;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Single point of entry for every outbound notification. Whether an event
 * goes out by email, in-app, or both is decided only by {@link #CHANNELS_BY_TYPE}
 * below — callers just say what happened, not how it should be delivered.
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    // EMAIL_VERIFICATION is deliberately both channels: the in-app reminder
    // stays visible (and useful) for as long as the account remains
    // unverified, which is exactly when a channel that doesn't depend on
    // the user having read their email matters most. WELCOME later covers
    // the "you're in" moment by email only — the reminder already served
    // as the in-app welcome, so a second in-app notification would be noise.
    private static final Map<NotificationType, Set<NotificationChannel>> CHANNELS_BY_TYPE = Map.of(
            NotificationType.EMAIL_VERIFICATION, EnumSet.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP),
            NotificationType.WELCOME, EnumSet.of(NotificationChannel.EMAIL),
            NotificationType.PASSWORD_RESET, EnumSet.of(NotificationChannel.EMAIL),
            NotificationType.ORDER_STATUS_CHANGED, EnumSet.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP),
            NotificationType.CHAT_REPLY, EnumSet.of(NotificationChannel.IN_APP),
            NotificationType.PRICE_DROP, EnumSet.of(NotificationChannel.IN_APP)
    );

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    public void dispatchEmailVerification(Long userId, String email, String name, String verificationToken) {
        deliver(NotificationType.EMAIL_VERIFICATION, userId,
                () -> emailService.sendVerificationEmail(email, name, verificationToken),
                () -> "Բարի գալուստ TechShop AM, " + name + "! Խնդրում ենք հաստատել ձեր էլ. հասցեն");
    }

    public void dispatchWelcome(Long userId, String email, String name) {
        deliver(NotificationType.WELCOME, userId,
                () -> emailService.sendWelcome(email, name),
                null);
    }

    public void dispatchPasswordReset(Long userId, String email, String name, String resetToken) {
        deliver(NotificationType.PASSWORD_RESET, userId,
                () -> emailService.sendPasswordResetEmail(email, name, resetToken),
                null);
    }

    public void dispatchOrderStatusChanged(Long userId, String email, String name, Long orderId,
                                            OrderStatus status, String note, BigDecimal totalPrice) {
        deliver(NotificationType.ORDER_STATUS_CHANGED, userId,
                () -> emailService.sendOrderStatusChanged(email, name, orderId, status, totalPrice),
                () -> orderStatusMessage(orderId, status));
    }

    // Not yet reachable in production: no producer publishes a chat-reply event
    // yet. Kept here so the type-to-channel mapping stays complete and testable.
    public void dispatchChatReply(Long userId, Long conversationId, String messagePreview) {
        deliver(NotificationType.CHAT_REPLY, userId, null,
                () -> "New reply in your support conversation: " + messagePreview);
    }

    // Not yet reachable in production: no producer publishes a price-drop event
    // yet. Kept here so the type-to-channel mapping stays complete and testable.
    public void dispatchPriceDrop(Long userId, Long productId, String productName, BigDecimal newPrice) {
        deliver(NotificationType.PRICE_DROP, userId, null,
                () -> productName + " just dropped to " + newPrice + " AMD — it's on your wishlist!");
    }

    private String orderStatusMessage(Long orderId, OrderStatus status) {
        if (status == OrderStatus.PENDING) {
            return "Your order #" + orderId + " has been created.";
        }
        return "Your order #" + orderId + " status changed to " + status + ".";
    }

    private void deliver(NotificationType type, Long userId, Runnable sendEmail, Supplier<String> inAppMessage) {
        Set<NotificationChannel> channels = CHANNELS_BY_TYPE.getOrDefault(type, Set.of());
        if (sendEmail != null && channels.contains(NotificationChannel.EMAIL)) {
            sendEmail.run();
        }
        if (inAppMessage != null && channels.contains(NotificationChannel.IN_APP)) {
            notificationRepository.save(Notification.builder().userId(userId).message(inAppMessage.get()).build());
        }
    }
}
