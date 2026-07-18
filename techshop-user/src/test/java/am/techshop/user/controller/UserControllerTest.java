package am.techshop.user.controller;

import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResendVerificationRequest;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.UserRole;
import am.techshop.common.exception.TechShopException;
import am.techshop.user.service.JwtService;
import am.techshop.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    // JwtAuthFilter (a Filter bean, so it's picked up by @WebMvcTest even
    // with security filters disabled at the MockMvc level) needs this to
    // construct — without it, context loading fails before any test runs.
    @MockBean
    private JwtService jwtService;

    @Test
    void register_ReturnsCreatedUser() throws Exception {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password");
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.USER, LocalDateTime.now(), true);
        AuthResponse authResponse = new AuthResponse("token", userResponse);
        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered"));
    }

    @Test
    void login_ReturnsToken() throws Exception {
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.USER, LocalDateTime.now(), true);
        AuthResponse authResponse = new AuthResponse("token", userResponse);
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void getAllUsers_ReturnsUserList() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.USER, LocalDateTime.now(), true);
        when(userService.getAllUsers()).thenReturn(List.of(userResponse));
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Mariam"));
    }

    @Test
    void getUserById_ReturnsUser() throws Exception {
        Long id = 1L;
        UserResponse userResponse = new UserResponse(id, "Mariam", "mariam@test.com", UserRole.USER, LocalDateTime.now(), true);
        when(userService.getUserById(id)).thenReturn(userResponse);
        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void verifyEmail_WhenTokenValid_ReturnsVerifiedUser() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.USER, LocalDateTime.now(), true);
        when(userService.verifyEmail("valid-token")).thenReturn(userResponse);

        mockMvc.perform(get("/api/users/verify-email").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailVerified").value(true));
    }

    @Test
    void verifyEmail_WhenTokenInvalid_ReturnsBadRequest() throws Exception {
        when(userService.verifyEmail("bad-token"))
                .thenThrow(new TechShopException("Invalid or already-used verification link", 400));

        mockMvc.perform(get("/api/users/verify-email").param("token", "bad-token"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendVerification_ReturnsOk() throws Exception {
        ResendVerificationRequest request = new ResendVerificationRequest("mariam@test.com");
        doNothing().when(userService).resendVerification(eq("mariam@test.com"));

        mockMvc.perform(post("/api/users/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}