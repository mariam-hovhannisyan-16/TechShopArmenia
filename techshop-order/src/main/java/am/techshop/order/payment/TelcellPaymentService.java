package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import org.springframework.stereotype.Service;

@Service
public class TelcellPaymentService extends AbstractSandboxPaymentProvider {

    private final TelcellProperties properties;

    public TelcellPaymentService(TelcellProperties properties) {
        super(PaymentMethod.TELCELL, "TELCELL");
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
