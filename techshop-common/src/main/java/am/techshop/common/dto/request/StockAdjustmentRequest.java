package am.techshop.common.dto.request;

import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull(message = "Quantity delta is required")
        Integer quantityDelta
) {}
