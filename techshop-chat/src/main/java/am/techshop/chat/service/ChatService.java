package am.techshop.chat.service;

import am.techshop.chat.security.ChatIdentity;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;

import java.util.List;

public interface ChatService {
    ConversationResponse getOrCreateConversation(ChatIdentity identity);
    List<MessageResponse> getMessages(ChatIdentity identity, Long conversationId);
    MessageResponse sendMessage(ChatIdentity identity, Long conversationId, SendMessageRequest request);
    List<ConversationResponse> getAllConversations();
}
