package am.techshop.chat.kafka;

import am.techshop.chat.service.ChatService;
import am.techshop.common.event.UserDeletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatEventConsumerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatEventConsumer consumer;

    @Test
    void handleUserDeleted_DeletesConversationsForUser() {
        UserDeletedEvent event = new UserDeletedEvent(1L);

        consumer.handleUserDeleted(event);

        verify(chatService).deleteConversationsForUser(1L);
    }
}
