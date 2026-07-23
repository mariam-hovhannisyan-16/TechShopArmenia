package am.techshop.user.controller;

import am.techshop.common.dto.request.ForgotPasswordRequest;
import am.techshop.common.dto.request.LoginRequest;
import am.techshop.common.dto.request.RegisterRequest;
import am.techshop.common.dto.request.ResendVerificationRequest;
import am.techshop.common.dto.request.ResetPasswordRequest;
import am.techshop.common.dto.request.RoleUpdateRequest;
import am.techshop.common.dto.response.AuthResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.UserRole;
import am.techshop.common.exception.TechShopException;
import am.techshop.user.config.JwtAuthFilter;
import am.techshop.user.config.SecurityConfig;
import am.techshop.user.service.CustomUserDetailsService;
import am.techshop.user.service.JwtService;
import am.techshop.user.service.UserService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    private static final Long USER_ID = 1L;

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void register_ReturnsCreatedUser() throws Exception {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", null);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        AuthResponse authResponse = new AuthResponse("token", userResponse);
        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered"));
    }

    @Test
    void register_WithAdminRole_PassesRoleThrough() throws Exception {
        RegisterRequest request = new RegisterRequest("Mariam", "mariam@test.com", "password", UserRole.ADMIN);
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.ADMIN, LocalDateTime.now(), true);
        AuthResponse authResponse = new AuthResponse("token", userResponse);
        when(userService.register(request)).thenReturn(authResponse);
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    void login_ReturnsToken() throws Exception {
        LoginRequest request = new LoginRequest("mariam@test.com", "password");
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        AuthResponse authResponse = new AuthResponse("token", userResponse);
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void getAllUsers_WhenCalledByAdmin_ReturnsUserList() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        when(userService.getAllUsers()).thenReturn(List.of(userResponse));
        mockMvc.perform(get("/api/users").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Mariam"));
    }

    @Test
    void getAllUsers_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users").with(authentication(asUser())))
                .andExpect(status().isForbidden());

        verify(userService, never()).getAllUsers();
    }

    @Test
    void getAllUsers_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).getAllUsers();
    }

    @Test
    void getUserById_ReturnsUser() throws Exception {
        Long id = 1L;
        UserResponse userResponse = new UserResponse(id, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        when(userService.getUserById(id)).thenReturn(userResponse);
        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void verifyEmail_WhenTokenValid_ReturnsVerifiedUser() throws Exception {
        UserResponse userResponse = new UserResponse(1L, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
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

    @Test
    void forgotPassword_ReturnsOk() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("mariam@test.com");
        doNothing().when(userService).forgotPassword(eq("mariam@test.com"));

        mockMvc.perform(post("/api/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_WhenTokenValid_ReturnsOk() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "newpassword");
        doNothing().when(userService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));
    }

    @Test
    void resetPassword_WhenTokenInvalid_ReturnsBadRequest() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("bad-token", "newpassword");
        doThrow(new TechShopException("Invalid or already-used reset link", 400))
                .when(userService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/api/users/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserRole_WhenCalledByAdmin_ReturnsUpdatedUser() throws Exception {
        Long id = 2L;
        RoleUpdateRequest request = new RoleUpdateRequest(UserRole.ADMIN);
        UserResponse userResponse = new UserResponse(id, "Mariam", "mariam@test.com", UserRole.ADMIN, LocalDateTime.now(), true);

        when(userService.updateUserRole(id, UserRole.ADMIN)).thenReturn(userResponse);

        mockMvc.perform(put("/api/users/{id}/role", id)
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    @Test
    void updateUserRole_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        RoleUpdateRequest request = new RoleUpdateRequest(UserRole.ADMIN);

        mockMvc.perform(put("/api/users/{id}/role", 2L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUserRole(any(), any());
    }

    @Test
    void updateUserRole_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        RoleUpdateRequest request = new RoleUpdateRequest(UserRole.ADMIN);

        mockMvc.perform(put("/api/users/{id}/role", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateUserRole(any(), any());
    }
}