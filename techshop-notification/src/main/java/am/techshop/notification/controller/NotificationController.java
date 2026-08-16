package am.techshop.notification.controller;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.NotificationResponse;
import am.techshop.notification.security.NotificationAccessGuard;
import am.techshop.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notifications", description = "In-app notifications for the authenticated user")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationAccessGuard notificationAccessGuard;

    @GetMapping("/api/notifications/{userId}")
    @Operation(
            summary = "List a user's notifications",
            description = "Returns 403 unless the caller is the owning user."
    )
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @Parameter(description = "ID of the notification owner", required = true)
            @PathVariable Long userId, Authentication authentication) {
        notificationAccessGuard.requireOwner(userId, authentication);
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getUserNotifications(userId)));
    }

    @PatchMapping("/api/notifications/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(description = "ID of the notification", required = true)
            @PathVariable Long id, Authentication authentication) {
        notificationService.markAsRead(notificationAccessGuard.currentUserId(authentication), id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }
}