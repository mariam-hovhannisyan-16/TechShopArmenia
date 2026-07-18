package am.techshop.common.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record WishlistResponse(
        Long id,
        Long userId,
        List<WishlistItemResponse> items,
        LocalDateTime createdAt
) {}
