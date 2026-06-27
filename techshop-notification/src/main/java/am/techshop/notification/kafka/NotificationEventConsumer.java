package am.techshop.notification.kafka;

import am.techshop.common.event.OrderCreatedEvent;
import am.techshop.common.event.UserRegisteredEvent;
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

    @KafkaListener(topics = "user-registered", groupId = "notification-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserRegisteredEvent"})
    public void handleUserRegistered(UserRegisteredEvent event) {
        notificationRepository.save(Notification.builder()
                .userId(event.userId())
                .message("Welcome to TechShopArmenia, " + event.name() + "!")
                .build());

        emailService.sendWelcome(event.email(), event.name());
    }
}