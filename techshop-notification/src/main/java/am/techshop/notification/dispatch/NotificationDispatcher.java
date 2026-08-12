package am.techshop.notification.dispatch;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.notification.i18n.OrderMessages;
import am.techshop.notification.mapper.NotificationMapper;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
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
            NotificationType.PRICE_DROP, EnumSet.of(NotificationChannel.IN_APP),
            NotificationType.ADMIN_NEW_USER, EnumSet.of(NotificationChannel.IN_APP)
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
                                            PaymentMethod paymentMethod, InstallmentPlanResponse installmentPlan,
                                            Language language, List<String> productNames) {
        deliver(NotificationType.ORDER_STATUS_CHANGED, userId,
                () -> emailService.sendOrderStatusChanged(email, name, orderId, status, totalPrice, paymentMethod, installmentPlan, language, productNames),
                () -> orderStatusMessage(orderId, status, note, language, productNames));
    }

    public void dispatchChatReply(Long userId, String messagePreview) {
        deliver(NotificationType.CHAT_REPLY, userId, null,
                () -> "New reply in your support conversation: %s".formatted(messagePreview));
    }

    public void dispatchPriceDrop(Long userId, String productName, BigDecimal oldPrice, BigDecimal newPrice) {
        deliver(NotificationType.PRICE_DROP, userId, null,
                () -> "%s-ի գինը իջել է. %s → %s".formatted(productName, oldPrice.toPlainString(), newPrice.toPlainString()));
    }

    public void dispatchAdminNewUser(List<Long> adminIds, String newUserName, String newUserEmail) {
        if (adminIds == null) {
            return;
        }
        String message = adminNewUserMessage(newUserName, newUserEmail);
        for (Long adminId : adminIds) {
            deliver(NotificationType.ADMIN_NEW_USER, adminId, null, () -> message);
        }
    }

    private String adminNewUserMessage(String name, String email) {
        // No per-admin language is stored, so include all three so the bell/panel
        // reads correctly whatever UI language the admin has selected.
        return "Նոր օգտատեր գրանցվել է՝ %1$s (%2$s) · New user registered: %1$s (%2$s) · Зарегистрировался новый пользователь: %1$s (%2$s)"
                .formatted(name, email);
    }

    private String orderStatusMessage(Long orderId, OrderStatus status, String note, Language language, List<String> productNames) {
        String base = OrderMessages.inAppBaseMessage(orderId, status, language, productNames);
        if (note != null && !note.isBlank()) {
            return base + " " + OrderMessages.inAppNoteLabel(language) + ": "
                    + OrderMessages.translateSystemNote(note, language);
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
