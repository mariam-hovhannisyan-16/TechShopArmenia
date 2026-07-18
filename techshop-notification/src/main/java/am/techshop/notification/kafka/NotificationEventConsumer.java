package am.techshop.notification.kafka;

import am.techshop.common.event.OrderCreatedEvent;
import am.techshop.common.event.UserRegisteredEvent;
import am.techshop.common.event.UserVerifiedEvent;
import am.techshop.notification.entity.Notification;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    @KafkaListener(topics = "order-created", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.OrderCreatedEvent"})
    public void handleOrderCreated(OrderCreatedEvent event) {
        notificationRepository.save(Notification.builder()
                .userId(event.userId())
                .message("Your order #" + event.orderId() + " has been created. Total: " + event.totalPrice() + " AMD")
                .build());

        emailService.sendOrderCreated(event.userEmail(), event.userName(), event.orderId(), event.totalPrice().toString());
    }

    // Registration no longer sends the welcome email directly — it sends a
    // verification link first. The welcome email now fires from
    // handleUserVerified below, once the user actually confirms their address.
    @KafkaListener(topics = "user-registered", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserRegisteredEvent"})
    public void handleUserRegistered(UserRegisteredEvent event) {
        notificationRepository.save(Notification.builder()
                .userId(event.userId())
                .message("Please verify your email to activate your TechShopArmenia account.")
                .build());

        emailService.sendVerificationEmail(event.email(), event.name(), event.verificationToken());
    }

    @KafkaListener(topics = "user-verified", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserVerifiedEvent"})
    public void handleUserVerified(UserVerifiedEvent event) {
        notificationRepository.save(Notification.builder()
                .userId(event.userId())
                .message("Welcome to TechShopArmenia, " + event.name() + "!")
                .build());

        emailService.sendWelcome(event.email(), event.name());
    }
}
