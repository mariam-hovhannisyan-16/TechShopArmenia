package am.techshop.common.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Address line is required")
        String line1,

        String line2,

        @NotBlank(message = "City is required")
        String city,

        String state,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        @NotBlank(message = "Country is required")
        String country
) {}
