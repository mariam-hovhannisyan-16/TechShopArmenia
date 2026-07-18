package am.techshop.order.payment;

import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.order.entity.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentProvidersTest {

    @Test
    void idramPaymentService_CreatesReferenceWithMethodPrefix() {
        IdramPaymentService provider = new IdramPaymentService();
        ReflectionTestUtils.setField(provider, "sandboxUrl", "https://sandbox.idram.am/payment");

        PaymentInitiationResult result = provider.createPayment(Order.builder().totalPrice(BigDecimal.TEN).build());

        assertEquals(PaymentMethod.IDRAM, provider.getMethod());
        assertTrue(result.paymentReference().startsWith("IDRAM-"));
        assertTrue(result.redirectUrl().startsWith("https://sandbox.idram.am/payment?ref=IDRAM-"));
        assertEquals(PaymentStatus.PENDING, result.status());
    }

    @Test
    void telcellPaymentService_CreatesReferenceWithMethodPrefix() {
        TelcellPaymentService provider = new TelcellPaymentService();
        ReflectionTestUtils.setField(provider, "sandboxUrl", "https://sandbox.telcellwallet.am/payment");

        PaymentInitiationResult result = provider.createPayment(Order.builder().totalPrice(BigDecimal.TEN).build());

        assertEquals(PaymentMethod.TELCELL, provider.getMethod());
        assertTrue(result.paymentReference().startsWith("TELCELL-"));
        assertTrue(result.redirectUrl().startsWith("https://sandbox.telcellwallet.am/payment?ref=TELCELL-"));
        assertEquals(PaymentStatus.PENDING, result.status());
    }

    @Test
    void verifyPayment_InSandboxMode_AlwaysApproves() {
        IdramPaymentService provider = new IdramPaymentService();
        ReflectionTestUtils.setField(provider, "sandboxMode", true);

        PaymentVerificationResult result = provider.verifyPayment("IDRAM-any-reference");

        assertEquals(PaymentStatus.PAID, result.status());
    }
}
