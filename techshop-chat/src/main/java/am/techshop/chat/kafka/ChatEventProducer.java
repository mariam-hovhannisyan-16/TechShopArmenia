package am.techshop.chat.kafka;

import am.techshop.common.event.ChatReplyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatEventProducer {

    private final KafkaTemplate<String, ChatReplyEvent> kafkaTemplate;

    public void sendChatReplyEvent(ChatReplyEvent event) {
        kafkaTemplate.send("chat-reply", event);
    }
}
