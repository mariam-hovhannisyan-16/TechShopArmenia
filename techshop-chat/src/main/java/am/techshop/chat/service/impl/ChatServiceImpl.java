package am.techshop.chat.service.impl;

import am.techshop.chat.entity.Conversation;
import am.techshop.chat.entity.Message;
import am.techshop.chat.mapper.ConversationMapper;
import am.techshop.chat.mapper.MessageMapper;
import am.techshop.chat.repository.ConversationRepository;
import am.techshop.chat.repository.MessageRepository;
import am.techshop.chat.security.ChatIdentity;
import am.techshop.chat.service.ChatService;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;
import am.techshop.common.enums.ConversationStatus;
import am.techshop.common.enums.MessageSender;
import am.techshop.common.exception.TechShopException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;

    public ConversationResponse getOrCreateConversation(ChatIdentity identity) {
        Conversation conversation = identity.isGuest()
                ? conversationRepository.findFirstByGuestSessionIdAndStatusOrderByIdDesc(identity.guestSessionId(), ConversationStatus.OPEN)
                        .orElseGet(() -> createConversation(identity))
                : conversationRepository.findFirstByUserIdAndStatusOrderByIdDesc(identity.userId(), ConversationStatus.OPEN)
                        .orElseGet(() -> createConversation(identity));
        return conversationMapper.toResponse(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getAllConversations() {
        return conversationRepository.findAllByOrderByIdDesc().stream()
                .map(conversationMapper::toResponse)
                .toList();
    }

    public List<MessageResponse> getMessages(ChatIdentity identity, Long conversationId) {
        getOwnedConversation(identity, conversationId);

        MessageSender counterpart = identity.admin() ? MessageSender.CUSTOMER : MessageSender.SUPPORT;
        List<Message> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        List<Message> unread = messages.stream()
                .filter(m -> m.getSender() == counterpart && !m.isRead())
                .toList();
        if (!unread.isEmpty()) {
            unread.forEach(m -> m.setRead(true));
            messageRepository.saveAll(unread);
        }

        return messages.stream().map(messageMapper::toResponse).toList();
    }

    public MessageResponse sendMessage(ChatIdentity identity, Long conversationId, SendMessageRequest request) {
        Conversation conversation = getOwnedConversation(identity, conversationId);
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new TechShopException("Conversation is closed", 409);
        }

        Message message = Message.builder()
                .conversationId(conversationId)
                .sender(identity.admin() ? MessageSender.SUPPORT : MessageSender.CUSTOMER)
                .text(request.text())
                .build();

        return messageMapper.toResponse(messageRepository.save(message));
    }

    private Conversation createConversation(ChatIdentity identity) {
        return conversationRepository.save(Conversation.builder()
                .userId(identity.userId())
                .guestSessionId(identity.guestSessionId())
                .status(ConversationStatus.OPEN)
                .build());
    }

    private Conversation getOwnedConversation(ChatIdentity identity, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new TechShopException("Conversation not found", 404));

        if (identity.admin()) {
            return conversation;
        }

        boolean owned = identity.isGuest()
                ? identity.guestSessionId().equals(conversation.getGuestSessionId())
                : identity.userId().equals(conversation.getUserId());

        if (!owned) {
            throw new TechShopException("Conversation not found", 404);
        }
        return conversation;
    }
}
