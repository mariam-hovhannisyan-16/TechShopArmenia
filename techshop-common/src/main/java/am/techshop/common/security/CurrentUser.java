package am.techshop.common.security;

import org.springframework.security.core.Authentication;

public final class CurrentUser {

    private CurrentUser() {}

    public static Long id(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
