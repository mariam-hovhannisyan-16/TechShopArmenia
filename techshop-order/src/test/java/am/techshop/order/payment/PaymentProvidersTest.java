package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.entity.InstallmentPlan;
import am.techshop.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentProvidersTest {

    @Test
    void idramPaymentService_CreatesReferenceWithMethodPrefix() {
        IdramPaymentService provider = new IdramPaymentService(
                new IdramProperties(true, "https://sandbox.idram.am/payment", "idram-merchant", "idram-secret"));

        Order order = new Order();
        order.setTotalPrice(BigDecimal.TEN);
        PaymentInitiationResult result = provider.createPayment(order);

        assertEquals(PaymentMethod.IDRAM, provider.getMethod());
        assertTrue(result.paymentReference().startsWith("IDRAM-"));
        assertTrue(result.redirectUrl().startsWith("https://sandbox.idram.am/payment?ref=IDRAM-"));
        assertTrue(result.redirectUrl().contains("&merchant_id=idram-merchant"));
        assertEquals(PaymentStatus.PENDING, result.status());
    }

    @Test
    void telcellPaymentService_CreatesReferenceWithMethodPrefix() {
        TelcellPaymentService provider = new TelcellPaymentService(
                new TelcellProperties(true, "https://sandbox.telcellwallet.am/payment", "telcell-merchant", "telcell-secret"));

        Order order = new Order();
        order.setTotalPrice(BigDecimal.TEN);
        PaymentInitiationResult result = provider.createPayment(order);

        assertEquals(PaymentMethod.TELCELL, provider.getMethod());
        assertTrue(result.paymentReference().startsWith("TELCELL-"));
        assertTrue(result.redirectUrl().startsWith("https://sandbox.telcellwallet.am/payment?ref=TELCELL-"));
        assertTrue(result.redirectUrl().contains("&merchant_id=telcell-merchant"));
        assertEquals(PaymentStatus.PENDING, result.status());
    }

    @Test
    void roketLinePaymentService_CreatesReferenceWithMethodPrefix() {
        RoketLinePaymentService provider = new RoketLinePaymentService(
                new RoketLineProperties(true, "https://sandbox.roketline.am/payment", "roket-merchant", "roket-secret"));

        Order order = new Order();
        order.setTotalPrice(BigDecimal.TEN);
        PaymentInitiationResult result = provider.createPayment(order);

        assertEquals(PaymentMethod.ROKET_LINE, provider.getMethod());
        assertTrue(result.paymentReference().startsWith("ROKET-LINE-"));
        assertTrue(result.redirectUrl().startsWith("https://sandbox.roketline.am/payment?ref=ROKET-LINE-"));
        assertTrue(result.redirectUrl().contains("&merchant_id=roket-merchant"));
        assertEquals(PaymentStatus.PENDING, result.status());
    }

    @Test
    void verifyPayment_InSandboxMode_AlwaysApproves() {
        IdramPaymentService provider = new IdramPaymentService(
                new IdramProperties(true, "https://sandbox.idram.am/payment", "idram-merchant", "idram-secret"));

        PaymentVerificationResult result = provider.verifyPayment("IDRAM-any-reference");

        assertEquals(PaymentStatus.PAID, result.status());
    }

    @Test
    void idramPaymentService_InLiveMode_ThrowsUnsupportedOperation() {
        IdramPaymentService provider = new IdramPaymentService(
                new IdramProperties(false, "https://sandbox.idram.am/payment", "idram-merchant", "idram-secret"));

        assertThrows(UnsupportedOperationException.class, () -> provider.verifyPayment("IDRAM-any-reference"));
    }

    @Test
    void telcellPaymentService_InLiveMode_ThrowsUnsupportedOperation() {
        TelcellPaymentService provider = new TelcellPaymentService(
                new TelcellProperties(false, "https://sandbox.telcellwallet.am/payment", "telcell-merchant", "telcell-secret"));

        assertThrows(UnsupportedOperationException.class, () -> provider.verifyPayment("TELCELL-any-reference"));
    }

    @Test
    void roketLinePaymentService_InLiveMode_ThrowsUnsupportedOperation() {
        RoketLinePaymentService provider = new RoketLinePaymentService(
                new RoketLineProperties(false, "https://sandbox.roketline.am/payment", "roket-merchant", "roket-secret"));

        assertThrows(UnsupportedOperationException.class, () -> provider.verifyPayment("ROKET-LINE-any-reference"));
    }

    @Test
    void telcellPaymentService_InSandboxMode_AlwaysApproves() {
        TelcellPaymentService provider = new TelcellPaymentService(
                new TelcellProperties(true, "https://sandbox.telcellwallet.am/payment", "telcell-merchant", "telcell-secret"));

        PaymentVerificationResult result = provider.verifyPayment("TELCELL-any-reference");

        assertEquals(PaymentStatus.PAID, result.status());
    }

    @Test
    void roketLinePaymentService_InSandboxMode_AlwaysApproves() {
        RoketLinePaymentService provider = new RoketLinePaymentService(
                new RoketLineProperties(true, "https://sandbox.roketline.am/payment", "roket-merchant", "roket-secret"));

        PaymentVerificationResult result = provider.verifyPayment("ROKET-LINE-any-reference");

        assertEquals(PaymentStatus.PAID, result.status());
    }

    private static InstallmentPaymentService installmentProvider() {
        return installmentProvider(true);
    }

    private static InstallmentPaymentService installmentProvider(boolean sandboxMode) {
        return new InstallmentPaymentService(
                new InstallmentProperties(sandboxMode, "https://sandbox.installments.am/payment", BigDecimal.valueOf(0.12)));
    }

    private static Order orderWithPlan(BigDecimal totalPrice, BigDecimal downPayment, int months) {
        Order order = new Order();
        order.setTotalPrice(totalPrice);
        InstallmentPlan plan = new InstallmentPlan();
        plan.setBankName("Ameriabank");
        plan.setDurationMonths(months);
        plan.setDownPayment(downPayment);
        order.setInstallmentPlan(plan);
        return order;
    }

    @Test
    void installmentPaymentService_CreatesReferenceWithMethodPrefix() {
        InstallmentPaymentService provider = installmentProvider();
        Order order = orderWithPlan(BigDecimal.valueOf(1200), BigDecimal.valueOf(200), 10);

        PaymentInitiationResult result = provider.createPayment(order);

        assertEquals(PaymentMethod.INSTALLMENT, provider.getMethod());
        assertTrue(result.paymentReference().startsWith("INSTALLMENT-"));
        assertTrue(result.redirectUrl().startsWith("https://sandbox.installments.am/payment?ref=INSTALLMENT-"));
        assertEquals(PaymentStatus.PENDING, result.status());
    }

    @ParameterizedTest
    @CsvSource({
            "1200, 200, 10, 110.00",
            "2400, 400, 6, 353.33"
    })
    void installmentPaymentService_ComputesRateAndMonthlyPaymentOnOrder(
            BigDecimal totalPrice, BigDecimal downPayment, int months, BigDecimal expectedMonthlyPayment) {
        InstallmentPaymentService provider = installmentProvider();
        Order order = orderWithPlan(totalPrice, downPayment, months);

        provider.createPayment(order);

        InstallmentPlan plan = order.getInstallmentPlan();
        assertEquals(BigDecimal.valueOf(0.12), plan.getRate());
        assertEquals(expectedMonthlyPayment, plan.getMonthlyPayment());
    }

    @Test
    void installmentPaymentService_WhenPlanMissing_ThrowsBadRequest() {
        InstallmentPaymentService provider = installmentProvider();
        Order order = new Order();
        order.setTotalPrice(BigDecimal.valueOf(1200));

        TechShopException ex = assertThrows(TechShopException.class, () -> provider.createPayment(order));

        assertEquals(400, ex.getStatusCode());
    }

    @Test
    void installmentPaymentService_WhenDownPaymentExceedsTotal_ThrowsBadRequest() {
        InstallmentPaymentService provider = installmentProvider();
        Order order = orderWithPlan(BigDecimal.valueOf(1000), BigDecimal.valueOf(1500), 10);

        TechShopException ex = assertThrows(TechShopException.class, () -> provider.createPayment(order));

        assertEquals(400, ex.getStatusCode());
    }

    @Test
    void installmentPaymentService_InSandboxMode_AlwaysApproves() {
        InstallmentPaymentService provider = installmentProvider(true);

        PaymentVerificationResult result = provider.verifyPayment("INSTALLMENT-any-reference");

        assertEquals(PaymentStatus.PAID, result.status());
    }

    @Test
    void installmentPaymentService_InLiveMode_ThrowsUnsupportedOperation() {
        InstallmentPaymentService provider = installmentProvider(false);

        assertThrows(UnsupportedOperationException.class, () -> provider.verifyPayment("INSTALLMENT-any-reference"));
    }
}
