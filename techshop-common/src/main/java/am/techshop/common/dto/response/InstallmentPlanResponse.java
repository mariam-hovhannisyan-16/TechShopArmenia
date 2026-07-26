package am.techshop.common.dto.response;

import java.math.BigDecimal;

public record InstallmentPlanResponse(
        String bankName,
        BigDecimal rate,
        int durationMonths,
        BigDecimal downPayment,
        BigDecimal monthlyPayment
) {}
