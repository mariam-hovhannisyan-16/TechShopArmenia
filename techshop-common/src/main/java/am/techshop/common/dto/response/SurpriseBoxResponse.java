package am.techshop.common.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record SurpriseBoxResponse(
        List<ProductResponse> items,
        BigDecimal totalPrice,
        BigDecimal remainingBudget
) {}
