package am.techshop.common.dto.response;

import java.time.LocalDateTime;

public record PricePredictionResponse(
        String prediction,
        String reason,
        LocalDateTime generatedAt
) {}
