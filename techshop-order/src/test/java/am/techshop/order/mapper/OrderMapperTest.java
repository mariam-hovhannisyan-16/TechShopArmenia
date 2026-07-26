package am.techshop.order.mapper;

import am.techshop.common.dto.request.AddressRequest;
import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.InstallmentDetailsRequest;
import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.order.entity.InstallmentPlan;
import am.techshop.order.entity.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapperImpl();

    private static AddressRequest sampleAddress() {
        return new AddressRequest("Mariam A", "+374000000", "1 Main St", null, "Yerevan", null, "0001", "Armenia");
    }

    @Test
    void toEntity_WithInstallmentDetails_MapsMonthsToDurationMonthsAndLeavesRateAndMonthlyPaymentUnset() {
        InstallmentDetailsRequest installmentDetails = new InstallmentDetailsRequest("Ameriabank", 12, BigDecimal.valueOf(50000));
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.INSTALLMENT, installmentDetails);

        Order order = mapper.toEntity(request, 1L, BigDecimal.valueOf(450000), mapper.toAddress(sampleAddress()), mapper.toAddress(sampleAddress()));

        assertEquals("Ameriabank", order.getInstallmentPlan().getBankName());
        assertEquals(12, order.getInstallmentPlan().getDurationMonths());
        assertEquals(BigDecimal.valueOf(50000), order.getInstallmentPlan().getDownPayment());
        assertNull(order.getInstallmentPlan().getRate());
        assertNull(order.getInstallmentPlan().getMonthlyPayment());
    }

    @Test
    void toEntity_WithoutInstallmentDetails_LeavesInstallmentPlanNull() {
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.IDRAM, null);

        Order order = mapper.toEntity(request, 1L, BigDecimal.valueOf(100), mapper.toAddress(sampleAddress()), mapper.toAddress(sampleAddress()));

        assertNull(order.getInstallmentPlan());
    }

    @Test
    void toResponse_WithInstallmentPlan_MapsAllFields() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setItems(java.util.List.of());
        order.setShippingAddress(mapper.toAddress(sampleAddress()));
        order.setBillingAddress(mapper.toAddress(sampleAddress()));
        order.setStatusHistory(java.util.List.of());
        InstallmentPlan plan = new InstallmentPlan();
        plan.setBankName("Ameriabank");
        plan.setRate(BigDecimal.valueOf(0.12));
        plan.setDurationMonths(12);
        plan.setDownPayment(BigDecimal.valueOf(50000));
        plan.setMonthlyPayment(BigDecimal.valueOf(37333.33));
        order.setInstallmentPlan(plan);

        OrderResponse response = mapper.toResponse(order);

        InstallmentPlanResponse planResponse = response.installmentPlan();
        assertEquals("Ameriabank", planResponse.bankName());
        assertEquals(12, planResponse.durationMonths());
        assertEquals(BigDecimal.valueOf(50000), planResponse.downPayment());
        assertEquals(BigDecimal.valueOf(37333.33), planResponse.monthlyPayment());
    }

    @Test
    void toResponse_WithoutInstallmentPlan_ReturnsNullInstallmentPlan() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setItems(java.util.List.of());
        order.setShippingAddress(mapper.toAddress(sampleAddress()));
        order.setBillingAddress(mapper.toAddress(sampleAddress()));
        order.setStatusHistory(java.util.List.of());

        OrderResponse response = mapper.toResponse(order);

        assertNull(response.installmentPlan());
    }
}
