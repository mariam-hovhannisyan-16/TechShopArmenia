package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RoketLinePaymentService extends AbstractSandboxPaymentProvider {

    @Value("${payment.roket-line.sandbox-mode:true}")
    private boolean sandboxMode;

    @Value("${payment.roket-line.sandbox-url}")
    private String sandboxUrl;

    public RoketLinePaymentService() {
        super(PaymentMethod.ROKET_LINE, "ROKET-LINE");
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
