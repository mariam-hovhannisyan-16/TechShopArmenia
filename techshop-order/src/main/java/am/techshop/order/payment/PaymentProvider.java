package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.order.entity.Order;

public interface PaymentProvider {
    PaymentMethod getMethod();
    PaymentInitiationResult createPayment(Order order);
    PaymentVerificationResult verifyPayment(String paymentReference);
}
