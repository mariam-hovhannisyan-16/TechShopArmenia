package am.techshop.common.dto.response;

import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long id,
        ProductResponse product,
        LocalDateTime addedAt
) {}
