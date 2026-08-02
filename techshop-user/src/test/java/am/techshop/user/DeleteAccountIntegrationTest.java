package am.techshop.user;

import am.techshop.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DeleteAccountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    private String registerAndGetToken(String email, String password) throws Exception {
        String body = """
                {"name":"Delete Me","email":"%s","password":"%s","role":null}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }

    @Test
    void deleteMe_WithValidTokenAndCorrectPassword_DeletesAccountFromDatabase() throws Exception {
        String email = "delete-me@test.com";
        String password = "password123";
        String token = registerAndGetToken(email, password);
        assertTrue(userRepository.findByEmail(email).isPresent());

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());

        assertTrue(userRepository.findByEmail(email).isEmpty());
    }

    @Test
    void deleteMe_WithWrongPassword_DoesNotDeleteAccount() throws Exception {
        String email = "keep-me@test.com";
        String token = registerAndGetToken(email, "password123");

        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"totally-wrong\"}"))
                .andExpect(status().isUnauthorized());

        assertTrue(userRepository.findByEmail(email).isPresent());
    }

    @Test
    void deleteMe_WithoutToken_ReturnsUnauthorizedAndDoesNotDelete() throws Exception {
        String email = "no-token@test.com";
        registerAndGetToken(email, "password123");

        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());

        assertTrue(userRepository.findByEmail(email).isPresent());
    }
}
