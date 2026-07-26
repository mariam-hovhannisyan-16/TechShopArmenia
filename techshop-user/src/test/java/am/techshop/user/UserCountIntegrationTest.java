package am.techshop.user;

import am.techshop.common.enums.UserRole;
import am.techshop.user.entity.User;
import am.techshop.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserCountIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void getUserCount_ReflectsActualRowsInDatabase() throws Exception {
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));

        userRepository.save(newUser("mariam@test.com"));
        userRepository.save(newUser("anna@test.com"));
        userRepository.save(newUser("gor@test.com"));

        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void getUserCount_IsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/count"))
                .andExpect(status().isOk());
    }

    private User newUser(String email) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(UserRole.CUSTOMER);
        return user;
    }
}
