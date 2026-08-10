package am.techshop.common.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        int stock,
        String category,
        String imageUrl,
        boolean isNew,
        Integer discountPercentage,
        List<StorageOptionResponse> storageOptions,
        List<String> simOptions,
        List<ColorVariantResponse> colorVariants,
        BigDecimal rating,
        Integer reviewCount
) {
    public ProductResponse(Long id, String name, String description, BigDecimal price, int stock,
                            String category, String imageUrl, boolean isNew, Integer discountPercentage) {
        this(id, name, description, price, stock, category, imageUrl, isNew, discountPercentage,
                List.of(), List.of(), List.of(), BigDecimal.ZERO, 0);
    }
}