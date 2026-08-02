package am.techshop.chat.kafka;

import am.techshop.chat.service.ChatService;
import am.techshop.common.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatEventConsumer {

    private final ChatService chatService;

    @KafkaListener(topics = "user-deleted", groupId = "chat-group",
            properties = {"spring.json.value.default.type=am.techshop.common.event.UserDeletedEvent"})
    public void handleUserDeleted(UserDeletedEvent event) {
        chatService.deleteConversationsForUser(event.userId());
    }
}
