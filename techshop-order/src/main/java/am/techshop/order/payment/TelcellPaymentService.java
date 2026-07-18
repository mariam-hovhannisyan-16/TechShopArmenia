package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.order.entity.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Sandbox/mock adapter for Telcell Wallet. Swap the method bodies for real HTTP calls
 * to Telcell's API once merchant credentials are available; the interface contract
 * (createPayment/verifyPayment) stays the same for callers.
 */
@Service
public class TelcellPaymentService implements PaymentProvider {

    @Value("${payment.telcell.sandbox-mode:true}")
    private boolean sandboxMode;

    @Value("${payment.telcell.sandbox-url}")
    private String sandboxUrl;

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.TELCELL;
    }

    @Override
    public PaymentInitiationResult createPayment(Order order) {
        String reference = "TELCELL-" + UUID.randomUUID();
        String redirectUrl = sandboxUrl + "?ref=" + reference + "&amount=" + order.getTotalPrice();
        return new PaymentInitiationResult(reference, redirectUrl, PaymentStatus.PENDING);
    }

    @Override
    public PaymentVerificationResult verifyPayment(String paymentReference) {
        if (sandboxMode) {
            return new PaymentVerificationResult(PaymentStatus.PAID, "Sandbox payment auto-approved");
        }
        throw new UnsupportedOperationException("Live Telcell verification is not yet implemented");
    }
}
