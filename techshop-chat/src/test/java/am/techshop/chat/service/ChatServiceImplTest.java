package am.techshop.chat.service;

import am.techshop.chat.dto.SendMessageResult;
import am.techshop.chat.entity.Conversation;
import am.techshop.chat.entity.Message;
import am.techshop.chat.kafka.ChatEventProducer;
import am.techshop.chat.mapper.ConversationMapper;
import am.techshop.chat.mapper.MessageMapper;
import am.techshop.chat.repository.ConversationRepository;
import am.techshop.chat.repository.MessageRepository;
import am.techshop.chat.security.ChatIdentity;
import am.techshop.chat.service.impl.ChatServiceImpl;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;
import am.techshop.common.enums.ConversationStatus;
import am.techshop.common.enums.MessageSender;
import am.techshop.common.event.ChatReplyEvent;
import am.techshop.common.exception.TechShopException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private ChatEventProducer chatEventProducer;

    @Mock
    private AiReplyService aiReplyService;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void getOrCreateConversation_ForLoggedInUser_ReturnsExistingOpenConversation() {
        ChatIdentity identity = new ChatIdentity(1L, null, false);
        Conversation existing = new Conversation();
        existing.setId(1L);
        existing.setUserId(1L);
        existing.setStatus(ConversationStatus.OPEN);
        ConversationResponse response = new ConversationResponse(1L, 1L, null, ConversationStatus.OPEN, LocalDateTime.now());

        when(conversationRepository.findFirstByUserIdAndStatusOrderByIdDesc(1L, ConversationStatus.OPEN))
                .thenReturn(Optional.of(existing));
        when(conversationMapper.toResponse(existing)).thenReturn(response);

        ConversationResponse result = chatService.getOrCreateConversation(identity);

        assertEquals(1L, result.id());
        verify(conversationRepository, never()).save(any());
    }

    @Test
    void getOrCreateConversation_ForGuestWithNoExisting_CreatesNewConversation() {
        ChatIdentity identity = new ChatIdentity(null, "guest-abc", false);
        Conversation newConversation = new Conversation();
        newConversation.setGuestSessionId("guest-abc");
        Conversation saved = new Conversation();
        saved.setId(2L);
        saved.setGuestSessionId("guest-abc");
        saved.setStatus(ConversationStatus.OPEN);
        ConversationResponse response = new ConversationResponse(2L, null, "guest-abc", ConversationStatus.OPEN, LocalDateTime.now());

        when(conversationRepository.findFirstByGuestSessionIdAndStatusOrderByIdDesc("guest-abc", ConversationStatus.OPEN))
                .thenReturn(Optional.empty());
        when(conversationMapper.toEntity(identity)).thenReturn(newConversation);
        when(conversationRepository.save(newConversation)).thenReturn(saved);
        when(conversationMapper.toResponse(saved)).thenReturn(response);

        ConversationResponse result = chatService.getOrCreateConversation(identity);

        assertEquals("guest-abc", result.guestSessionId());
        verify(conversationRepository).save(newConversation);
    }

    @Test
    void getMessages_WhenNotOwner_ThrowsNotFound() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.OPEN);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        ChatIdentity otherUser = new ChatIdentity(2L, null, false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> chatService.getMessages(otherUser, 1L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void getMessages_WhenConversationMissing_ThrowsNotFound() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.empty());

        ChatIdentity identity = new ChatIdentity(1L, null, false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> chatService.getMessages(identity, 1L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void getMessages_AsCustomer_MarksSupportMessagesRead() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.OPEN);
        Message supportMsg = new Message();
        supportMsg.setId(10L);
        supportMsg.setConversationId(1L);
        supportMsg.setSender(MessageSender.SUPPORT);
        supportMsg.setText("hi");
        supportMsg.setRead(false);
        Message customerMsg = new Message();
        customerMsg.setId(11L);
        customerMsg.setConversationId(1L);
        customerMsg.setSender(MessageSender.CUSTOMER);
        customerMsg.setText("hello");
        customerMsg.setRead(false);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(supportMsg, customerMsg));
        when(messageMapper.toResponse(any(Message.class))).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.SUPPORT, "hi", true, LocalDateTime.now()));

        ChatIdentity identity = new ChatIdentity(1L, null, false);
        List<MessageResponse> result = chatService.getMessages(identity, 1L);

        assertEquals(2, result.size());
        assertTrue(supportMsg.isRead());
        assertFalse(customerMsg.isRead());
        verify(messageRepository).saveAll(List.of(supportMsg));
    }

    @Test
    void getMessages_AsAdmin_BypassesOwnershipAndMarksCustomerMessagesRead() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(99L);
        conversation.setStatus(ConversationStatus.OPEN);
        Message customerMsg = new Message();
        customerMsg.setId(11L);
        customerMsg.setConversationId(1L);
        customerMsg.setSender(MessageSender.CUSTOMER);
        customerMsg.setText("hello");
        customerMsg.setRead(false);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(customerMsg));
        when(messageMapper.toResponse(any(Message.class))).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.CUSTOMER, "hello", true, LocalDateTime.now()));

        ChatIdentity admin = new ChatIdentity(5L, null, true);
        List<MessageResponse> result = chatService.getMessages(admin, 1L);

        assertEquals(1, result.size());
        assertTrue(customerMsg.isRead());
    }

    @Test
    void sendMessage_AsCustomer_SavesCustomerMessage() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("Hello");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.CUSTOMER);
        newMessage.setText("Hello");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.CUSTOMER, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.CUSTOMER, "Hello", false, LocalDateTime.now()));

        ChatIdentity identity = new ChatIdentity(1L, null, false);
        SendMessageResult result = chatService.sendMessage(identity, 1L, request);

        assertEquals(MessageSender.CUSTOMER, result.message().sender());
        assertEquals("Hello", result.message().text());
        verify(chatEventProducer, never()).sendChatReplyEvent(any());
    }

    @Test
    void sendMessage_AsCustomer_GeneratesAndSavesBotReplyImmediately() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("Do you deliver to Gyumri?");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.CUSTOMER);
        newMessage.setText("Do you deliver to Gyumri?");
        Message botMessage = new Message();
        botMessage.setSender(MessageSender.BOT);
        botMessage.setText("Yes, delivery to Gyumri takes 2-5 business days.");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.CUSTOMER, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.CUSTOMER, "Do you deliver to Gyumri?", false, LocalDateTime.now()));
        when(messageRepository.existsByConversationIdAndSender(1L, MessageSender.SUPPORT)).thenReturn(false);
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(newMessage));
        when(aiReplyService.generateReply(List.of(newMessage)))
                .thenReturn(Optional.of("Yes, delivery to Gyumri takes 2-5 business days."));
        when(messageRepository.save(argThat(m -> m != null && m.getSender() == MessageSender.BOT))).thenReturn(botMessage);
        when(messageMapper.toResponse(botMessage)).thenReturn(
                new MessageResponse(2L, 1L, MessageSender.BOT, "Yes, delivery to Gyumri takes 2-5 business days.", false, LocalDateTime.now()));

        ChatIdentity identity = new ChatIdentity(1L, null, false);
        SendMessageResult result = chatService.sendMessage(identity, 1L, request);

        assertEquals(MessageSender.CUSTOMER, result.message().sender());
        assertNotNull(result.botReply());
        assertEquals(MessageSender.BOT, result.botReply().sender());
        assertEquals("Yes, delivery to Gyumri takes 2-5 business days.", result.botReply().text());
    }

    @Test
    void sendMessage_AsCustomer_WhenAdminHasAlreadyReplied_SkipsBotReply() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("Any update on my order?");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.CUSTOMER);
        newMessage.setText("Any update on my order?");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.CUSTOMER, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.CUSTOMER, "Any update on my order?", false, LocalDateTime.now()));
        when(messageRepository.existsByConversationIdAndSender(1L, MessageSender.SUPPORT)).thenReturn(true);

        ChatIdentity identity = new ChatIdentity(1L, null, false);
        SendMessageResult result = chatService.sendMessage(identity, 1L, request);

        assertEquals(MessageSender.CUSTOMER, result.message().sender());
        assertNull(result.botReply());
        verify(aiReplyService, never()).generateReply(any());
        verify(messageRepository, never()).findByConversationIdOrderByCreatedAtAsc(1L);
    }

    @Test
    void sendMessage_AsCustomer_WhenAiReplyDisabled_ReturnsNullBotReply() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("Hello");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.CUSTOMER);
        newMessage.setText("Hello");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.CUSTOMER, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.CUSTOMER, "Hello", false, LocalDateTime.now()));
        when(messageRepository.findByConversationIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(newMessage));
        when(aiReplyService.generateReply(List.of(newMessage))).thenReturn(Optional.empty());

        ChatIdentity identity = new ChatIdentity(1L, null, false);
        SendMessageResult result = chatService.sendMessage(identity, 1L, request);

        assertEquals(MessageSender.CUSTOMER, result.message().sender());
        assertNull(result.botReply());
    }

    @Test
    void sendMessage_AsAdmin_SavesSupportMessage() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(99L);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("We can help");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.SUPPORT);
        newMessage.setText("We can help");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.SUPPORT, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.SUPPORT, "We can help", false, LocalDateTime.now()));

        ChatIdentity admin = new ChatIdentity(5L, null, true);
        SendMessageResult result = chatService.sendMessage(admin, 1L, request);

        assertEquals(MessageSender.SUPPORT, result.message().sender());
        assertNull(result.botReply());
    }

    @Test
    void sendMessage_AsAdminReplyingToRegisteredUser_PublishesChatReplyEvent() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(99L);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("It shipped yesterday!");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.SUPPORT);
        newMessage.setText("It shipped yesterday!");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.SUPPORT, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.SUPPORT, "It shipped yesterday!", false, LocalDateTime.now()));

        ChatIdentity admin = new ChatIdentity(5L, null, true);
        chatService.sendMessage(admin, 1L, request);

        verify(chatEventProducer).sendChatReplyEvent(new ChatReplyEvent(99L, 1L, "It shipped yesterday!"));
    }

    @Test
    void sendMessage_AsAdminReplyingToGuestConversation_DoesNotPublishEvent() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setGuestSessionId("guest-abc");
        conversation.setUserId(null);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("We can help");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.SUPPORT);
        newMessage.setText("We can help");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.SUPPORT, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.SUPPORT, "We can help", false, LocalDateTime.now()));

        ChatIdentity admin = new ChatIdentity(5L, null, true);
        chatService.sendMessage(admin, 1L, request);

        verify(chatEventProducer, never()).sendChatReplyEvent(any());
    }

    @Test
    void sendMessage_WhenEventPublishFails_StillReturnsSavedMessage() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(99L);
        conversation.setStatus(ConversationStatus.OPEN);
        SendMessageRequest request = new SendMessageRequest("We can help");
        Message newMessage = new Message();
        newMessage.setSender(MessageSender.SUPPORT);
        newMessage.setText("We can help");

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageMapper.toEntity(1L, MessageSender.SUPPORT, request)).thenReturn(newMessage);
        when(messageRepository.save(newMessage)).thenReturn(newMessage);
        when(messageMapper.toResponse(newMessage)).thenReturn(
                new MessageResponse(1L, 1L, MessageSender.SUPPORT, "We can help", false, LocalDateTime.now()));
        doThrow(new RuntimeException("kafka unavailable"))
                .when(chatEventProducer).sendChatReplyEvent(any());

        ChatIdentity admin = new ChatIdentity(5L, null, true);
        SendMessageResult result = chatService.sendMessage(admin, 1L, request);

        assertEquals(MessageSender.SUPPORT, result.message().sender());
    }

    @Test
    void sendMessage_WhenConversationClosed_ThrowsConflict() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.CLOSED);
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        ChatIdentity identity = new ChatIdentity(1L, null, false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> chatService.sendMessage(identity, 1L, new SendMessageRequest("Hello")));

        assertEquals(409, ex.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getAllConversations_ReturnsAllMappedConversations() {
        Conversation conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUserId(1L);
        conversation.setStatus(ConversationStatus.OPEN);
        when(conversationRepository.findAllByOrderByIdDesc()).thenReturn(List.of(conversation));
        when(conversationMapper.toResponse(conversation)).thenReturn(
                new ConversationResponse(1L, 1L, null, ConversationStatus.OPEN, LocalDateTime.now()));

        List<ConversationResponse> result = chatService.getAllConversations();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
