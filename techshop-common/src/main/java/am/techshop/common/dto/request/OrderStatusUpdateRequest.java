package am.techshop.common.dto.request;

import am.techshop.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull(message = "Status is required")
        OrderStatus status,

        String note
) {}
