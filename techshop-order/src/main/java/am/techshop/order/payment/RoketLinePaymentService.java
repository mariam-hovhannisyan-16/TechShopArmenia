package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import org.springframework.stereotype.Service;

@Service
public class RoketLinePaymentService extends AbstractSandboxPaymentProvider {

    private final RoketLineProperties properties;

    public RoketLinePaymentService(RoketLineProperties properties) {
        super(PaymentMethod.ROKET_LINE, "ROKET-LINE");
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
