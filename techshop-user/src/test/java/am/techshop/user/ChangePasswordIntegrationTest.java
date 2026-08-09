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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ChangePasswordIntegrationTest {

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
                {"name":"Change Me","email":"%s","password":"%s","role":null}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("data").get("token").asText();
    }

    private void login(String email, String password) throws Exception {
        String body = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_WithValidTokenAndCorrectCurrentPassword_AllowsLoginWithNewPasswordOnly() throws Exception {
        String email = "change-me@test.com";
        String oldPassword = "old-password";
        String newPassword = "new-password";
        String token = registerAndGetToken(email, oldPassword);

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + oldPassword + "\",\"newPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk());

        login(email, newPassword);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + oldPassword + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_WithWrongCurrentPassword_DoesNotChangePasswordAndOldPasswordStillWorks() throws Exception {
        String email = "keep-old-password@test.com";
        String oldPassword = "old-password";
        String token = registerAndGetToken(email, oldPassword);

        mockMvc.perform(put("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"totally-wrong\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isBadRequest());

        login(email, oldPassword);
    }

    @Test
    void changePassword_WithoutToken_ReturnsUnauthorized() throws Exception {
        String email = "no-token@test.com";
        String oldPassword = "old-password";
        registerAndGetToken(email, oldPassword);

        mockMvc.perform(put("/api/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + oldPassword + "\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isUnauthorized());

        login(email, oldPassword);
    }
}
