package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import org.springframework.stereotype.Service;

@Service
public class IdramPaymentService extends AbstractSandboxPaymentProvider {

    private final IdramProperties properties;

    public IdramPaymentService(IdramProperties properties) {
        super(PaymentMethod.IDRAM, "IDRAM");
        this.properties = properties;
    }

    @Override
    protected boolean isSandboxMode() {
        return properties.sandboxMode();
    }

    @Override
    protected String getSandboxUrl() {
        return properties.sandboxUrl();
    }

    @Override
    protected String getMerchantId() {
        return properties.merchantId();
    }
}
