package am.techshop.common.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {}
