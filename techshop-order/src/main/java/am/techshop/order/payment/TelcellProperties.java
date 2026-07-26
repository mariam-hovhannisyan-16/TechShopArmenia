package am.techshop.order.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "payment.telcell")
public record TelcellProperties(
        @DefaultValue("true") boolean sandboxMode,
        String sandboxUrl,
        String merchantId,
        String secretKey) {
}
