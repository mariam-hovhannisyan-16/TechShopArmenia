package am.techshop.common.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RepairEntryResponse(
        Long id,
        String description,
        LocalDate entryDate,
        LocalDateTime createdAt
) {}
