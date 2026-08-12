package am.techshop.chat.service.impl;

import am.techshop.chat.dto.SendMessageResult;
import am.techshop.chat.entity.Conversation;
import am.techshop.chat.entity.Message;
import am.techshop.chat.kafka.ChatEventProducer;
import am.techshop.chat.mapper.ConversationMapper;
import am.techshop.chat.mapper.MessageMapper;
import am.techshop.chat.repository.ConversationRepository;
import am.techshop.chat.repository.MessageRepository;
import am.techshop.chat.security.ChatIdentity;
import am.techshop.chat.service.AiReplyService;
import am.techshop.chat.service.ChatService;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;
import am.techshop.common.enums.ConversationStatus;
import am.techshop.common.enums.MessageSender;
import am.techshop.common.event.ChatReplyEvent;
import am.techshop.common.exception.TechShopException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ChatEventProducer chatEventProducer;
    private final AiReplyService aiReplyService;

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
        Set<Long> escalatedIds = Set.copyOf(messageRepository.findDistinctConversationIdBySender(MessageSender.SUPPORT));
        return conversationRepository.findAllByOrderByIdDesc().stream()
                .map(conversation -> conversationMapper.toResponse(conversation, escalatedIds.contains(conversation.getId())))
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

    public SendMessageResult sendMessage(ChatIdentity identity, Long conversationId, SendMessageRequest request) {
        Conversation conversation = getOwnedConversation(identity, conversationId);
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new TechShopException("Conversation is closed", 409);
        }

        Message message = messageMapper.toEntity(conversationId,
                identity.admin() ? MessageSender.SUPPORT : MessageSender.CUSTOMER, request);

        MessageResponse response = messageMapper.toResponse(messageRepository.save(message));

        MessageResponse botReply = null;
        if (identity.admin() && conversation.getUserId() != null) {
            notifyCustomerOfReply(conversation.getUserId(), conversationId, request.text());
        } else if (!identity.admin()) {
            boolean adminHasReplied = messageRepository.existsByConversationIdAndSender(conversationId, MessageSender.SUPPORT);
            if (!adminHasReplied) {
                botReply = autoReply(conversation).orElse(null);
            }
        }

        return new SendMessageResult(response, botReply);
    }

    private Optional<MessageResponse> autoReply(Conversation conversation) {
        List<Message> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId());
        return aiReplyService.generateReply(history).map(text -> {
            Message botMessage = new Message();
            botMessage.setConversationId(conversation.getId());
            botMessage.setSender(MessageSender.BOT);
            botMessage.setText(text);
            return messageMapper.toResponse(messageRepository.save(botMessage));
        });
    }

    private void notifyCustomerOfReply(Long userId, Long conversationId, String messagePreview) {
        try {
            chatEventProducer.sendChatReplyEvent(new ChatReplyEvent(userId, conversationId, messagePreview));
        } catch (Exception ex) {
            log.warn("Failed to publish chat-reply notification for conversation {}: {}", conversationId, ex.getMessage());
        }
    }

    public void deleteConversationsForUser(Long userId) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId);
        if (conversations.isEmpty()) {
            return;
        }
        List<Long> conversationIds = conversations.stream().map(Conversation::getId).toList();
        messageRepository.deleteByConversationIdIn(conversationIds);
        conversationRepository.deleteAll(conversations);
    }

    private Conversation createConversation(ChatIdentity identity) {
        return conversationRepository.save(conversationMapper.toEntity(identity));
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
