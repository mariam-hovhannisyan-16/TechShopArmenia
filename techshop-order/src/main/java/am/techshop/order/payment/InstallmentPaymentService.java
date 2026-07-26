package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.entity.InstallmentPlan;
import am.techshop.order.entity.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class InstallmentPaymentService extends AbstractSandboxPaymentProvider {

    private final InstallmentProperties properties;

    public InstallmentPaymentService(InstallmentProperties properties) {
        super(PaymentMethod.INSTALLMENT, "INSTALLMENT");
        this.properties = properties;
    }

    @Override
    public PaymentInitiationResult createPayment(Order order) {
        applyPlan(order);
        return super.createPayment(order);
    }

    private void applyPlan(Order order) {
        InstallmentPlan plan = order.getInstallmentPlan();
        if (plan == null) {
            throw new TechShopException("Installment plan details are required for INSTALLMENT payments", 400);
        }

        BigDecimal principal = order.getTotalPrice().subtract(plan.getDownPayment());
        if (principal.compareTo(BigDecimal.ZERO) < 0) {
            throw new TechShopException("Down payment cannot exceed the order total", 400);
        }

        BigDecimal termFraction = BigDecimal.valueOf(plan.getDurationMonths())
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal totalInterest = principal.multiply(properties.annualRate()).multiply(termFraction)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyPayment = principal.add(totalInterest)
                .divide(BigDecimal.valueOf(plan.getDurationMonths()), 2, RoundingMode.HALF_UP);

        plan.setRate(properties.annualRate());
        plan.setMonthlyPayment(monthlyPayment);
    }

    @Override
    protected boolean isSandboxMode() {
        return properties.sandboxMode();
    }

    @Override
    protected String getSandboxUrl() {
        return properties.sandboxUrl();
    }
}
