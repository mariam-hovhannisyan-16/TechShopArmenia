package am.techshop.common.event;

import am.techshop.common.enums.Language;

public record UserVerifiedEvent(
        Long userId,
        String email,
        String name,
        Language language
) {}
