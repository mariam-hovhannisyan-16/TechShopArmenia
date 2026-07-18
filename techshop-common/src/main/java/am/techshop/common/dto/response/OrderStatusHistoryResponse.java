package am.techshop.common.dto.response;

import am.techshop.common.enums.OrderStatus;

import java.time.LocalDateTime;

public record OrderStatusHistoryResponse(
        OrderStatus status,
        String note,
        LocalDateTime changedAt
) {}
