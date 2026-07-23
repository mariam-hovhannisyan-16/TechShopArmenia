package am.techshop.common.dto.request;

import am.techshop.common.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(
        @NotNull(message = "Role is required")
        UserRole role
) {}
