package am.techshop.common.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message text is required")
        @Size(max = 2000, message = "Message text must not exceed 2000 characters")
        String text
) {}
