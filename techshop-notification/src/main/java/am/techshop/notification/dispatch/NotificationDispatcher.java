package am.techshop.notification.dispatch;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.notification.mapper.NotificationMapper;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

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
    private final NotificationMapper notificationMapper;

    public void dispatchEmailVerification(Long userId, String email, String name, String verificationToken) {
        deliver(NotificationType.EMAIL_VERIFICATION, userId,
                () -> emailService.sendVerificationEmail(email, name, verificationToken),
                () -> "Բարի գալուստ TechShop AM, %s! Խնդրում ենք հաստատել ձեր էլ. հասցեն".formatted(name));
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
                                            OrderStatus status, String note, BigDecimal totalPrice,
                                            PaymentMethod paymentMethod, InstallmentPlanResponse installmentPlan) {
        deliver(NotificationType.ORDER_STATUS_CHANGED, userId,
                () -> emailService.sendOrderStatusChanged(email, name, orderId, status, totalPrice, paymentMethod, installmentPlan),
                () -> orderStatusMessage(orderId, status, note));
    }

    public void dispatchChatReply(Long userId, String messagePreview) {
        deliver(NotificationType.CHAT_REPLY, userId, null,
                () -> "New reply in your support conversation: %s".formatted(messagePreview));
    }

    public void dispatchPriceDrop(Long userId, String productName, BigDecimal newPrice) {
        deliver(NotificationType.PRICE_DROP, userId, null,
                () -> "%s just dropped to %s AMD — it's on your wishlist!".formatted(productName, newPrice));
    }

    private String orderStatusMessage(Long orderId, OrderStatus status, String note) {
        String base = status == OrderStatus.PENDING
                ? "Your order #%s has been created.".formatted(orderId)
                : "Your order #%s status changed to %s.".formatted(orderId, status);
        if (note != null && !note.isBlank()) {
            return base + " Note: " + note;
        }
        return base;
    }

    private void deliver(NotificationType type, Long userId, Runnable sendEmail, Supplier<String> inAppMessage) {
        Set<NotificationChannel> channels = CHANNELS_BY_TYPE.getOrDefault(type, Set.of());
        if (sendEmail != null && channels.contains(NotificationChannel.EMAIL)) {
            sendEmail.run();
        }
        if (inAppMessage != null && channels.contains(NotificationChannel.IN_APP)) {
            notificationRepository.save(notificationMapper.toEntity(userId, inAppMessage.get()));
        }
    }
}
