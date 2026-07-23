package am.techshop.common.event;

public record PasswordResetRequestedEvent(
        Long userId,
        String email,
        String name,
        String resetToken
) {}
