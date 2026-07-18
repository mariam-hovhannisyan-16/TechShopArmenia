package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.order.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sandbox/mock adapter for the Idram payment gateway. Swap the method bodies for real
 * HTTP calls to Idram's API once merchant credentials are available; the interface
 * contract (createPayment/verifyPayment) stays the same for callers.
 */
@Service
public class IdramPaymentService implements PaymentProvider {

    @Value("${payment.idram.sandbox-mode:true}")
    private boolean sandboxMode;

    @Value("${payment.idram.sandbox-url}")
    private String sandboxUrl;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.IDRAM;
    }

    @Override
    public PaymentInitiationResult createPayment(Order order) {
        String reference = "IDRAM-" + UUID.randomUUID();
        String redirectUrl = sandboxUrl + "?ref=" + reference + "&amount=" + order.getTotalPrice();
        return new PaymentInitiationResult(reference, redirectUrl, PaymentStatus.PENDING);
    }

    @Override
    public PaymentVerificationResult verifyPayment(String paymentReference) {
        if (sandboxMode) {
            return new PaymentVerificationResult(PaymentStatus.PAID, "Sandbox payment auto-approved");
        }
        throw new UnsupportedOperationException("Live Idram verification is not yet implemented");
    }
}
