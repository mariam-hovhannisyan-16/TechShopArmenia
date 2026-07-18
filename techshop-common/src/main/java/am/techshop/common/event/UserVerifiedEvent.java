package am.techshop.common.event;

public record UserVerifiedEvent(
        Long userId,
        String email,
        String name
) {}
