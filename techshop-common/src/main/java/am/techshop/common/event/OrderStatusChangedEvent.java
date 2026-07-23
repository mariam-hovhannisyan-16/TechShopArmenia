package am.techshop.common.event;

import am.techshop.common.enums.OrderStatus;

import java.math.BigDecimal;

public record OrderStatusChangedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String userName,
        OrderStatus status,
        String note,
        BigDecimal totalPrice
) {}
