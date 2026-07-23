package am.techshop.common.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DiscountUpdateRequest(
        @Min(value = 0, message = "Discount percentage cannot be negative")
        @Max(value = 100, message = "Discount percentage cannot exceed 100")
        Integer discountPercentage
) {}
