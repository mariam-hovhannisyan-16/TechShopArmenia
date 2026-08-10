package am.techshop.common.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Internal, service-to-service request: techshop-order recomputes a product's rating from its
 * own Review rows (the source of truth) and pushes the full, authoritative snapshot here rather
 * than sending a delta - techshop-product just caches whatever it's told.
 */
public record RatingUpdateRequest(
        @NotNull(message = "Rating is required")
        BigDecimal rating,

        @NotNull(message = "Review count is required")
        @Min(value = 0, message = "Review count cannot be negative")
        Integer reviewCount
) {}
