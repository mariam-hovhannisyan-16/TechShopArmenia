package am.techshop.user.kafka;

import am.techshop.common.event.PasswordResetRequestedEvent;
import am.techshop.common.event.UserRegisteredEvent;
import am.techshop.common.event.UserVerifiedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        kafkaTemplate.send("user-registered", event);
    }

    public void sendUserVerifiedEvent(UserVerifiedEvent event) {
        kafkaTemplate.send("user-verified", event);
    }

    public void sendPasswordResetRequestedEvent(PasswordResetRequestedEvent event) {
        kafkaTemplate.send("password-reset-requested", event);
    }
}
