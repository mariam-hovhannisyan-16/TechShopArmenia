package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.exception.TechShopException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentProviderFactory {

    private final Map<PaymentMethod, PaymentProvider> providersByMethod;

    public PaymentProviderFactory(List<PaymentProvider> providers) {
        this.providersByMethod = providers.stream()
                .collect(Collectors.toMap(PaymentProvider::getMethod, Function.identity()));
    }

    public PaymentProvider resolve(PaymentMethod method) {
        PaymentProvider provider = providersByMethod.get(method);
        if (provider == null) {
            throw new TechShopException("Unsupported payment method: " + method, 400);
        }
        return provider;
    }
}
