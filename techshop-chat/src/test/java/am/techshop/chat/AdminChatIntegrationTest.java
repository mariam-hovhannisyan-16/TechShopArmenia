package am.techshop.chat;

import am.techshop.chat.repository.ConversationRepository;
import am.techshop.chat.repository.MessageRepository;
import am.techshop.common.dto.request.SendMessageRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminChatIntegrationTest {

    private static final Long CUSTOMER_ID = 100L;
    private static final Long ADMIN_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @BeforeEach
    void cleanDatabase() {
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
    }

    private Authentication asCustomer() {
        return new UsernamePasswordAuthenticationToken(CUSTOMER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(ADMIN_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void customerMessage_IsVisibleToAdmin_AndAdminReply_IsVisibleToCustomer() throws Exception {
        MvcResult startResult = mockMvc.perform(post("/api/chat/conversations").with(authentication(asCustomer())))
                .andExpect(status().isCreated())
                .andReturn();
        Long conversationId = extractDataId(startResult);

        mockMvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .with(authentication(asCustomer()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("Where is my order?"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message.sender").value("CUSTOMER"));

        mockMvc.perform(get("/api/admin/chat/conversations").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == " + conversationId + ")]").exists());

        mockMvc.perform(get("/api/chat/conversations/{id}/messages", conversationId).with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sender").value("CUSTOMER"))
                .andExpect(jsonPath("$.data[0].text").value("Where is my order?"));

        mockMvc.perform(post("/api/chat/conversations/{id}/messages", conversationId)
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessageRequest("It shipped yesterday!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message.sender").value("SUPPORT"));

        MvcResult customerViewResult = mockMvc.perform(get("/api/chat/conversations/{id}/messages", conversationId)
                        .with(authentication(asCustomer())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();

        JsonNode messages = objectMapper.readTree(customerViewResult.getResponse().getContentAsString()).path("data");
        assertEquals("CUSTOMER", messages.get(0).path("sender").asText());
        assertEquals("SUPPORT", messages.get(1).path("sender").asText());
        assertEquals("It shipped yesterday!", messages.get(1).path("text").asText());
    }

    @Test
    void adminEndpoints_AsRegularUser_ReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/chat/conversations").with(authentication(asCustomer())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoints_WithoutAuthentication_ReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/chat/conversations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listConversations_AsAdmin_OnlyReturnsRealConversationsFromDatabase() throws Exception {
        assertTrue(conversationRepository.findAll().isEmpty());

        mockMvc.perform(post("/api/chat/conversations").with(authentication(asCustomer())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/chat/conversations").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        assertEquals(1, conversationRepository.findAll().size());
    }

    private Long extractDataId(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return data.path("id").asLong();
    }
}
