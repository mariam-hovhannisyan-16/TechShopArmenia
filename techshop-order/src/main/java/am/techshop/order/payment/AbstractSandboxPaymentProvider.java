package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.order.entity.Order;

import java.util.UUID;

/**
 * Shared sandbox/mock behavior for payment adapters: generates a prefixed reference
 * and redirect URL, and auto-approves verification while in sandbox mode. Concrete
 * providers supply their method, reference prefix, and their own @Value-injected
 * config (each needs distinct property keys, e.g. payment.idram.* vs
 * payment.telcell.*, so those fields can't live here).
 */
public abstract class AbstractSandboxPaymentProvider implements PaymentProvider {

    private final PaymentMethod method;
    private final String referencePrefix;

    protected AbstractSandboxPaymentProvider(PaymentMethod method, String referencePrefix) {
        this.method = method;
        this.referencePrefix = referencePrefix;
    }

    @Override
    public PaymentMethod getMethod() {
        return method;
    }

    @Override
    public PaymentInitiationResult createPayment(Order order) {
        String reference = referencePrefix + "-" + UUID.randomUUID();
        String redirectUrl = getSandboxUrl() + "?ref=" + reference + "&amount=" + order.getTotalPrice();
        return new PaymentInitiationResult(reference, redirectUrl, PaymentStatus.PENDING);
    }

    @Override
    public PaymentVerificationResult verifyPayment(String paymentReference) {
        if (isSandboxMode()) {
            return new PaymentVerificationResult(PaymentStatus.PAID, "Sandbox payment auto-approved");
        }
        throw new UnsupportedOperationException("Live " + method + " verification is not yet implemented");
    }

    protected abstract boolean isSandboxMode();

    protected abstract String getSandboxUrl();
}
