package am.techshop.common.dto.response;

import java.time.LocalDate;

public record DigitalTwinSummaryResponse(
        Long id,
        String productName,
        LocalDate purchaseDate,
        LocalDate warrantyEndDate,
        boolean warrantyActive
) {}
