package am.techshop.common.dto.request;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferencesUpdateRequest(
        @NotNull(message = "notifyPriceDrops is required")
        Boolean notifyPriceDrops
) {}
