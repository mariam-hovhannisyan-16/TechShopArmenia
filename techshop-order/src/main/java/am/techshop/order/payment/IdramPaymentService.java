package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
