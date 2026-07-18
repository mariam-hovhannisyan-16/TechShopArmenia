package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sandbox/mock adapter for Telcell Wallet. Swap isSandboxMode/getSandboxUrl for real
 * HTTP calls to Telcell's API once merchant credentials are available; the interface
 * contract (createPayment/verifyPayment) stays the same for callers.
 */
@Service
public class TelcellPaymentService extends AbstractSandboxPaymentProvider {

    @Value("${payment.telcell.sandbox-mode:true}")
    private boolean sandboxMode;

    @Value("${payment.telcell.sandbox-url}")
    private String sandboxUrl;

    public TelcellPaymentService() {
        super(PaymentMethod.TELCELL, "TELCELL");
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
