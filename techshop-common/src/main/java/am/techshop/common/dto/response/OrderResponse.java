package am.techshop.common.dto.response;

import am.techshop.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        List<OrderItemResponse> items,
        BigDecimal totalPrice,
        OrderStatus status,
        LocalDateTime createdAt
) {}