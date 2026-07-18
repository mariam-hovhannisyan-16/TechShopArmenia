package am.techshop.notification.service;

import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.notification.entity.Notification;
import am.techshop.notification.mapper.NotificationMapper;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void getUserNotifications_ReturnsNotificationList() {
        Long userId = 1L;
        Notification notification = Notification.builder()
                .id(1L)
                .userId(userId)
                .message("Your order has been created")
                .read(false)
                .build();
        NotificationResponse response = new NotificationResponse(1L, userId, "Your order has been created", false, LocalDateTime.now());

        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(notification));
        when(notificationMapper.toResponse(notification)).thenReturn(response);

        List<NotificationResponse> result = notificationService.getUserNotifications(userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).userId());
        verify(notificationRepository).findByUserId(userId);
    }

    @Test
    void getUserNotifications_WhenNoNotifications_ReturnsEmptyList() {
        Long userId = 1L;
        when(notificationRepository.findByUserId(userId)).thenReturn(List.of());

        List<NotificationResponse> result = notificationService.getUserNotifications(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void markAsRead_WhenNotificationExists_MarksAsRead() {
        Long id = 1L;
        Notification notification = Notification.builder()
                .id(id)
                .userId(1L)
                .message("Test")
                .read(false)
                .build();

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(id);

        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_WhenNotificationNotFound_ThrowsException() {
        Long id = 1L;
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> notificationService.markAsRead(id));

        assertEquals(404, ex.getStatusCode());
    }
}