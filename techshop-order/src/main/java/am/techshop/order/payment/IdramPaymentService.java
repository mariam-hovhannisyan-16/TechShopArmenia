package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sandbox/mock adapter for the Idram payment gateway. Swap isSandboxMode/getSandboxUrl
 * for real HTTP calls to Idram's API once merchant credentials are available; the
 * interface contract (createPayment/verifyPayment) stays the same for callers.
 */
@Service
public class IdramPaymentService extends AbstractSandboxPaymentProvider {

    @Value("${payment.idram.sandbox-mode:true}")
    private boolean sandboxMode;

    @Value("${payment.idram.sandbox-url}")
    private String sandboxUrl;

    public IdramPaymentService() {
        super(PaymentMethod.IDRAM, "IDRAM");
    }

    @Override
    protected boolean isSandboxMode() {
        return sandboxMode;
    }

    @Override
    protected String getSandboxUrl() {
        return sandboxUrl;
    }
}
