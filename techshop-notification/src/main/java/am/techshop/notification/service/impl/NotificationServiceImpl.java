package am.techshop.notification.service.impl;

import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.notification.entity.Notification;
import am.techshop.notification.mapper.NotificationMapper;
import am.techshop.notification.repository.NotificationRepository;
import am.techshop.common.exception.TechShopException;
import am.techshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public void markAsRead(Long userId, Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new TechShopException("Notification not found", 404));
        if (!notification.getUserId().equals(userId)) {
            throw new TechShopException("You do not have permission to access this resource", 403);
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    public void deleteNotificationsForUser(Long userId) {
        notificationRepository.deleteByUserId(userId);
    }
}