package am.techshop.common.event;

import am.techshop.common.enums.Language;

public record ChatReplyEvent(
        Long userId,
        Long conversationId,
        String messagePreview,
        Language language
) {}
