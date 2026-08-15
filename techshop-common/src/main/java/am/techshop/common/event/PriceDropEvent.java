package am.techshop.common.event;

import am.techshop.common.enums.Language;

import java.math.BigDecimal;

public record PriceDropEvent(
        Long userId,
        Long productId,
        String productName,
        BigDecimal oldPrice,
        BigDecimal newPrice,
        Language language
) {}
