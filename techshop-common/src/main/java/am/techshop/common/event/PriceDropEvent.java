package am.techshop.common.event;

import java.math.BigDecimal;

public record PriceDropEvent(
        Long userId,
        Long productId,
        String productName,
        BigDecimal oldPrice,
        BigDecimal newPrice
) {}
