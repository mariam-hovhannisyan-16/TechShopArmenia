package am.techshop.common.dto.response;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        String category,
        String imageUrl,
        boolean isNew
) {}