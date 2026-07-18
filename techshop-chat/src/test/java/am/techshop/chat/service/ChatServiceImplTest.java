package am.techshop.chat.service;

import am.techshop.chat.entity.Conversation;
import am.techshop.chat.entity.Message;
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
import am.techshop.common.exception.TechShopException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    @InjectMocks
    private ChatServiceImpl chatService;

    @Test
    void getOrCreateConversation_ForLoggedInUser_ReturnsExistingOpenConversation() {
        ChatIdentity identity = new ChatIdentity(1L, null, false);
        Conversation existing = Conversation.builder().id(1L).userId(1L).status(ConversationStatus.OPEN).build();
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
        Conversation saved = Conversation.builder().id(2L).guestSessionId("guest-abc").status(ConversationStatus.OPEN).build();
        ConversationResponse response = new ConversationResponse(2L, null, "guest-abc", ConversationStatus.OPEN, LocalDateTime.now());

        when(conversationRepository.findFirstByGuestSessionIdAndStatusOrderByIdDesc("guest-abc", ConversationStatus.OPEN))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(saved);
        when(conversationMapper.toResponse(saved)).thenReturn(response);

        ConversationResponse result = chatService.getOrCreateConversation(identity);

        assertEquals("guest-abc", result.guestSessionId());

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(captor.capture());
        assertEquals("guest-abc", captor.getValue().getGuestSessionId());
        assertEquals(ConversationStatus.OPEN, captor.getValue().getStatus());
    }

    @Test
    void getMessages_WhenNotOwner_ThrowsNotFound() {
        Conversation conversation = Conversation.builder().id(1L).userId(1L).status(ConversationStatus.OPEN).build();
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
        Conversation conversation = Conversation.builder().id(1L).userId(1L).status(ConversationStatus.OPEN).build();
        Message supportMsg = Message.builder().id(10L).conversationId(1L).sender(MessageSender.SUPPORT).text("hi").read(false).build();
        Message customerMsg = Message.builder().id(11L).conversationId(1L).sender(MessageSender.CUSTOMER).text("hello").read(false).build();

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
        Conversation conversation = Conversation.builder().id(1L).userId(99L).status(ConversationStatus.OPEN).build();
        Message customerMsg = Message.builder().id(11L).conversationId(1L).sender(MessageSender.CUSTOMER).text("hello").read(false).build();

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
        Conversation conversation = Conversation.builder().id(1L).userId(1L).status(ConversationStatus.OPEN).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageMapper.toResponse(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return new MessageResponse(1L, 1L, m.getSender(), m.getText(), false, LocalDateTime.now());
        });

        ChatIdentity identity = new ChatIdentity(1L, null, false);
        MessageResponse result = chatService.sendMessage(identity, 1L, new SendMessageRequest("Hello"));

        assertEquals(MessageSender.CUSTOMER, result.sender());
        assertEquals("Hello", result.text());
    }

    @Test
    void sendMessage_AsAdmin_SavesSupportMessage() {
        Conversation conversation = Conversation.builder().id(1L).userId(99L).status(ConversationStatus.OPEN).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messageMapper.toResponse(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            return new MessageResponse(1L, 1L, m.getSender(), m.getText(), false, LocalDateTime.now());
        });

        ChatIdentity admin = new ChatIdentity(5L, null, true);
        MessageResponse result = chatService.sendMessage(admin, 1L, new SendMessageRequest("We can help"));

        assertEquals(MessageSender.SUPPORT, result.sender());
    }

    @Test
    void sendMessage_WhenConversationClosed_ThrowsConflict() {
        Conversation conversation = Conversation.builder().id(1L).userId(1L).status(ConversationStatus.CLOSED).build();
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        ChatIdentity identity = new ChatIdentity(1L, null, false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> chatService.sendMessage(identity, 1L, new SendMessageRequest("Hello")));

        assertEquals(409, ex.getStatusCode());
        verify(messageRepository, never()).save(any());
    }

    @Test
    void getAllConversations_ReturnsAllMappedConversations() {
        Conversation conversation = Conversation.builder().id(1L).userId(1L).status(ConversationStatus.OPEN).build();
        when(conversationRepository.findAllByOrderByIdDesc()).thenReturn(List.of(conversation));
        when(conversationMapper.toResponse(conversation)).thenReturn(
                new ConversationResponse(1L, 1L, null, ConversationStatus.OPEN, LocalDateTime.now()));

        List<ConversationResponse> result = chatService.getAllConversations();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
