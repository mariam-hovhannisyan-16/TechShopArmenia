package am.techshop.common.event;

public record ChatReplyEvent(
        Long userId,
        Long conversationId,
        String messagePreview
) {}
