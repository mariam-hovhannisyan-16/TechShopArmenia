package am.techshop.common.dto.response;

public record AddressResponse(
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
) {}
