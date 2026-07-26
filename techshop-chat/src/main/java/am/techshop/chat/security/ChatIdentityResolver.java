package am.techshop.chat.security;

import am.techshop.common.exception.TechShopException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class ChatIdentityResolver {

    public ChatIdentity resolve(Authentication authentication, String guestSessionId) {
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);
            return new ChatIdentity(userId, null, isAdmin);
        }
        if (guestSessionId != null && !guestSessionId.isBlank()) {
            return new ChatIdentity(null, guestSessionId, false);
        }
        throw new TechShopException("Authentication or X-Guest-Session-Id header is required", 400);
    }

    public ChatIdentity resolveAdmin(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Long userId)) {
            throw new TechShopException("Authentication is required", 401);
        }
        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new TechShopException("Admin role is required", 403);
        }
        return new ChatIdentity(userId, null, true);
    }
}
