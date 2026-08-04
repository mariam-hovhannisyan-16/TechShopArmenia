package am.techshop.common.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DigitalTwinDetailResponse(
        Long id,
        String productName,
        Long productId,
        LocalDate purchaseDate,
        LocalDate warrantyEndDate,
        boolean warrantyActive,
        String notes,
        List<RepairEntryResponse> repairs
) {}
