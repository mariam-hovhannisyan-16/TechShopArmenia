package am.techshop.common.dto.response;

import am.techshop.common.enums.ConversationStatus;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long userId,
        String guestSessionId,
        ConversationStatus status,
        LocalDateTime createdAt,
        boolean escalated
) {}
