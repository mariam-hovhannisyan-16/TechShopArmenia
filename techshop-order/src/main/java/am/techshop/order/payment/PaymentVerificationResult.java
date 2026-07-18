package am.techshop.order.payment;

import am.techshop.common.enums.PaymentStatus;

public record PaymentVerificationResult(
        PaymentStatus status,
        String message
) {}
