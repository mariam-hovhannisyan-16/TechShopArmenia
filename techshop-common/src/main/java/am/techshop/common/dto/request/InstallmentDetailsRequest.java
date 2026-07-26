package am.techshop.common.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InstallmentDetailsRequest(
        @NotBlank(message = "Bank name is required")
        String bankName,

        @Min(value = 1, message = "Duration must be at least 1 month")
        int months,

        @NotNull(message = "Down payment is required")
        @DecimalMin(value = "0.0", message = "Down payment cannot be negative")
        BigDecimal downPayment
) {}
