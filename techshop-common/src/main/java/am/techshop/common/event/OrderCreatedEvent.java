package am.techshop.common.event;

import java.math.BigDecimal;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String userName,
        BigDecimal totalPrice
) {}