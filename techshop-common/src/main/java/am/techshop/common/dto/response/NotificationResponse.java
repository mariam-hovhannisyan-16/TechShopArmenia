package am.techshop.common.dto.response;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long userId,
        String message,
        boolean read,
        LocalDateTime createdAt
) {}