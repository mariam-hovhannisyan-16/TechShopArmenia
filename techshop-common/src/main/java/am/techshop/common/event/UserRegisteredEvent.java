package am.techshop.common.event;

import am.techshop.common.enums.Language;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String name,
        String verificationToken,
        Language language
) {}
