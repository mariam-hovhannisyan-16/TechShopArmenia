package am.techshop.notification.service;

import am.techshop.common.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getUserNotifications(Long userId);
    void markAsRead(Long userId, Long id);
}