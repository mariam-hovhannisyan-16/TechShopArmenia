package am.techshop.common.event;

public record UserRegisteredEvent(
        Long userId,
        String email,
        String name
) {}