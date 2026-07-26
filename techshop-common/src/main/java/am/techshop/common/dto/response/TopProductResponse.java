package am.techshop.common.dto.response;

public record TopProductResponse(
        Long productId,
        String productName,
        long quantitySold
) {}
