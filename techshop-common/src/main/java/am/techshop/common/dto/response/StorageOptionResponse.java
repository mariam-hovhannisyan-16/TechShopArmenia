package am.techshop.common.dto.response;

import java.math.BigDecimal;

public record StorageOptionResponse(
        String label,
        BigDecimal priceDelta
) {}
