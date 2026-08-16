package am.techshop.notification.dispatch;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.dto.response.UserLanguageResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.notification.client.UserClient;
import am.techshop.notification.i18n.AccountMessages;
import am.techshop.notification.i18n.AuthMessages;
import am.techshop.notification.i18n.OrderMessages;
import am.techshop.notification.mapper.NotificationMapper;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
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
    private final UserClient userClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public void dispatchEmailVerification(Long userId, String email, String name, String verificationToken, Language language) {
        Language resolvedLanguage = orDefault(language);
        deliver(NotificationType.EMAIL_VERIFICATION, userId,
                () -> emailService.sendVerificationEmail(email, name, verificationToken, resolvedLanguage),
                () -> AuthMessages.inAppEmailVerification(name, resolvedLanguage));
    }

    public void dispatchWelcome(Long userId, String email, String name, Language language) {
        Language resolvedLanguage = orDefault(language);
        deliver(NotificationType.WELCOME, userId,
                () -> emailService.sendWelcome(email, name, resolvedLanguage),
                null);
    }

    public void dispatchPasswordReset(Long userId, String email, String name, String resetToken, Language language) {
        Language resolvedLanguage = orDefault(language);
        deliver(NotificationType.PASSWORD_RESET, userId,
                () -> emailService.sendPasswordResetEmail(email, name, resetToken, resolvedLanguage),
                null);
    }

    private Language orDefault(Language language) {
        return language != null ? language : Language.HY;
    }

    public void dispatchOrderStatusChanged(Long userId, String email, String name, Long orderId,
                                            OrderStatus status, String note, BigDecimal totalPrice,
                                            PaymentMethod paymentMethod, InstallmentPlanResponse installmentPlan,
                                            Language language, List<String> productNames) {
        deliver(NotificationType.ORDER_STATUS_CHANGED, userId,
                () -> emailService.sendOrderStatusChanged(email, name, orderId, status, totalPrice, paymentMethod, installmentPlan, language, productNames),
                () -> orderStatusMessage(orderId, status, note, language, productNames));
    }

    public void dispatchChatReply(Long userId, String messagePreview, Language language) {
        deliver(NotificationType.CHAT_REPLY, userId, null,
                () -> AccountMessages.chatReplyMessage(messagePreview, language));
    }

    public void dispatchPriceDrop(Long userId, String productName, BigDecimal oldPrice, BigDecimal newPrice, Language language) {
        deliver(NotificationType.PRICE_DROP, userId, null,
                () -> AccountMessages.priceDropMessage(productName, oldPrice, newPrice, language));
    }

    public void dispatchAdminNewUser(List<Long> adminIds, String newUserName, String newUserEmail) {
        if (adminIds == null || adminIds.isEmpty()) {
            return;
        }
        Map<Long, Language> languagesByAdminId = resolveLanguages(adminIds);
        for (Long adminId : adminIds) {
            Language language = languagesByAdminId.getOrDefault(adminId, Language.HY);
            String message = AccountMessages.adminNewUserMessage(newUserName, newUserEmail, language);
            deliver(NotificationType.ADMIN_NEW_USER, adminId, null, () -> message);
        }
    }

    private Map<Long, Language> resolveLanguages(List<Long> userIds) {
        try {
            List<UserLanguageResponse> languages = userClient.getUserLanguages(userIds, internalApiKey).data();
            if (languages == null) {
                return Map.of();
            }
            return languages.stream().collect(Collectors.toMap(UserLanguageResponse::userId, UserLanguageResponse::language));
        } catch (Exception ex) {
            log.warn("Failed to resolve languages for admin new-user notification: {}", ex.getMessage());
            return Map.of();
        }
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
