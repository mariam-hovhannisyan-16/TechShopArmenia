package am.techshop.notification.controller;

import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.notification.config.SecurityConfig;
import am.techshop.notification.security.JwtAuthFilter;
import am.techshop.notification.security.JwtService;
import am.techshop.notification.security.NotificationAccessGuard;
import am.techshop.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, NotificationAccessGuard.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtService jwtService;

    private Authentication asUser(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of());
    }

    @Test
    void getUserNotifications_AsOwner_ReturnsNotifications() throws Exception {
        Long userId = 1L;
        NotificationResponse response = new NotificationResponse(1L, userId, "Test", false, LocalDateTime.now());

        when(notificationService.getUserNotifications(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications/{userId}", userId).with(authentication(asUser(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(userId));
    }

    @Test
    void getUserNotifications_AsDifferentUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/notifications/{userId}", 1L).with(authentication(asUser(2L))))
                .andExpect(status().isForbidden());

        verify(notificationService, never()).getUserNotifications(1L);
    }

    @Test
    void getUserNotifications_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notifications/{userId}", 1L))
                .andExpect(status().isUnauthorized());

        verify(notificationService, never()).getUserNotifications(1L);
    }

    @Test
    void markAsRead_AsAuthenticatedUser_ReturnsSuccess() throws Exception {
        Long id = 1L;
        Long userId = 1L;
        doNothing().when(notificationService).markAsRead(userId, id);

        mockMvc.perform(patch("/api/notifications/{id}/read", id).with(authentication(asUser(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Marked as read"));
    }

    @Test
    void markAsRead_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/notifications/{id}/read", 1L))
                .andExpect(status().isUnauthorized());

        verify(notificationService, never()).markAsRead(any(), any());
    }
}
