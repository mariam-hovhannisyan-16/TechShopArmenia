package am.techshop.common.event;

import am.techshop.common.enums.Language;

public record PasswordResetRequestedEvent(
        Long userId,
        String email,
        String name,
        String resetToken,
        Language language
) {}
