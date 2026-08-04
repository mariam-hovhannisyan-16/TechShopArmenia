package am.techshop.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AddRepairEntryRequest(
        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Date is required")
        LocalDate date
) {}
