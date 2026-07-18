package am.techshop.common.dto.response;

import am.techshop.common.enums.MessageSender;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        MessageSender sender,
        String text,
        boolean read,
        LocalDateTime createdAt
) {}
