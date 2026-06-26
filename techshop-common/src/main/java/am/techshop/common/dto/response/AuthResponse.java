package am.techshop.common.dto.response;

public record AuthResponse(
        String token,
        UserResponse user
) {}