package am.techshop.chat.controller;

import am.techshop.chat.config.SecurityConfig;
import am.techshop.chat.security.ChatIdentity;
import am.techshop.chat.security.ChatIdentityResolver;
import am.techshop.chat.security.JwtAuthFilter;
import am.techshop.chat.security.JwtService;
import am.techshop.chat.service.ChatService;
import am.techshop.common.dto.request.SendMessageRequest;
import am.techshop.common.dto.response.ConversationResponse;
import am.techshop.common.dto.response.MessageResponse;
import am.techshop.common.enums.ConversationStatus;
import am.techshop.common.enums.MessageSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, ChatIdentityResolver.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static ConversationResponse sampleConversation() {
        return new ConversationResponse(1L, USER_ID, null, ConversationStatus.OPEN, LocalDateTime.now());
    }

    private static MessageResponse sampleMessage(MessageSender sender) {
        return new MessageResponse(1L, 1L, sender, "hello", false, LocalDateTime.now());
    }

    @Test
    void startConversation_AsLoggedInUser_ReturnsConversation() throws Exception {
        when(chatService.getOrCreateConversation(any(ChatIdentity.class))).thenReturn(sampleConversation());

        mockMvc.perform(post("/api/chat/conversations").with(authentication(asUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(USER_ID));
    }

    @Test
    void startConversation_AsGuest_ReturnsConversation() throws Exception {
        ConversationResponse guestConversation = new ConversationResponse(2L, null, "guest-abc", ConversationStatus.OPEN, LocalDateTime.now());
        when(chatService.getOrCreateConversation(any(ChatIdentity.class))).thenReturn(guestConversation);

        mockMvc.perform(post("/api/chat/conversations").header("X-Guest-Session-Id", "guest-abc"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.guestSessionId").value("guest-abc"));
    }

    @Test
    void startConversation_WithoutAuthOrGuestHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/chat/conversations"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMessages_AsGuest_ReturnsMessageList() throws Exception {
        when(chatService.getMessages(any(ChatIdentity.class), eq(1L))).thenReturn(List.of(sampleMessage(MessageSender.CUSTOMER)));

        mockMvc.perform(get("/api/chat/conversations/{id}/messages", 1L).header("X-Guest-Session-Id", "guest-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sender").value("CUSTOMER"));
    }

    @Test
    void sendMessage_AsAdmin_ReturnsSupportMessage() throws Exception {
        SendMessageRequest request = new SendMessageRequest("We're looking into it");
        when(chatService.sendMessage(any(ChatIdentity.class), eq(1L), any(SendMessageRequest.class)))
                .thenReturn(sampleMessage(MessageSender.SUPPORT));

        mockMvc.perform(post("/api/chat/conversations/{id}/messages", 1L)
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sender").value("SUPPORT"));
    }

    @Test
    void sendMessage_WithBlankText_ReturnsBadRequest() throws Exception {
        SendMessageRequest request = new SendMessageRequest("");

        mockMvc.perform(post("/api/chat/conversations/{id}/messages", 1L)
                        .header("X-Guest-Session-Id", "guest-abc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
