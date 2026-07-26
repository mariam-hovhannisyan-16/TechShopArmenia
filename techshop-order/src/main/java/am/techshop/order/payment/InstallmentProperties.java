package am.techshop.order.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "payment.installment")
public record InstallmentProperties(
        @DefaultValue("true") boolean sandboxMode,
        String sandboxUrl,
        @DefaultValue("0.12") BigDecimal annualRate) {
}
