package am.techshop.common.dto.response;

import am.techshop.common.enums.Language;

public record UserLanguageResponse(
        Long userId,
        Language language
) {}
