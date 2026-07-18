package am.techshop.notification.controller;

import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @Test
    void getUserNotifications_ReturnsNotifications() throws Exception {
        Long userId = 1L;
        NotificationResponse response = new NotificationResponse(1L, userId, "Test", false, LocalDateTime.now());

        when(notificationService.getUserNotifications(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notifications/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(userId));
    }

    @Test
    void markAsRead_ReturnsSuccess() throws Exception {
        Long id = 1L;
        doNothing().when(notificationService).markAsRead(id);

        mockMvc.perform(patch("/api/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Marked as read"));
    }
}