package am.techshop.common.event;

import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;

public record OrderStatusChangedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String userName,
        OrderStatus status,
        String note,
        BigDecimal totalPrice,
        PaymentMethod paymentMethod,
        InstallmentPlanResponse installmentPlan,
        Language language,
        List<String> productNames
) {}
