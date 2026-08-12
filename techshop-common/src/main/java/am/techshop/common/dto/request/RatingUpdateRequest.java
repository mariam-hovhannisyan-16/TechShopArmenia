package am.techshop.common.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RatingUpdateRequest(
        @NotNull(message = "Rating is required")
        BigDecimal rating,

        @NotNull(message = "Review count is required")
        @Min(value = 0, message = "Review count cannot be negative")
        Integer reviewCount
) {}
