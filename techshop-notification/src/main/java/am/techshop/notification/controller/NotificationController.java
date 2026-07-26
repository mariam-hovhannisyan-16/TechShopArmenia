package am.techshop.notification.controller;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.notification.security.NotificationAccessGuard;
import am.techshop.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationAccessGuard notificationAccessGuard;

    @GetMapping("/api/notifications/{userId}")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @PathVariable Long userId, Authentication authentication) {
        notificationAccessGuard.requireOwner(userId, authentication);
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getUserNotifications(userId)));
    }

    @PatchMapping("/api/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(notificationAccessGuard.currentUserId(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }
}