package am.techshop.common.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String productName,
        BigDecimal productPrice,
        int quantity,
        BigDecimal totalPrice
) {}