package am.techshop.common.dto.request;

import am.techshop.common.enums.Language;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        Language language
) {}
