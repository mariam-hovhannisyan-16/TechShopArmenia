package am.techshop.common.dto.response;

import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        List<OrderItemResponse> items,
        BigDecimal totalPrice,
        OrderStatus status,
        AddressResponse shippingAddress,
        AddressResponse billingAddress,
        String notes,
        List<OrderStatusHistoryResponse> statusHistory,
        PaymentMethod paymentMethod,
        String paymentReference,
        PaymentStatus paymentStatus,
        String paymentRedirectUrl,
        InstallmentPlanResponse installmentPlan,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
