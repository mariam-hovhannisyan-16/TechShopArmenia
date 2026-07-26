package am.techshop.common.dto.request;

import am.techshop.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CheckoutRequest(
        @NotNull(message = "Shipping address is required")
        @Valid
        AddressRequest shippingAddress,

        @NotNull(message = "Billing address is required")
        @Valid
        AddressRequest billingAddress,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @Valid
        InstallmentDetailsRequest installmentDetails
) {}
