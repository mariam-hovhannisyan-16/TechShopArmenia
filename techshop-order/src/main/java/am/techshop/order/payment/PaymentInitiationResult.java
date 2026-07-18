package am.techshop.order.payment;

import am.techshop.common.enums.PaymentStatus;

public record PaymentInitiationResult(
        String paymentReference,
        String redirectUrl,
        PaymentStatus status
) {}
