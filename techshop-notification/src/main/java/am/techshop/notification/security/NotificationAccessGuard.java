package am.techshop.notification.security;

import am.techshop.common.exception.TechShopException;
import am.techshop.common.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class NotificationAccessGuard {

    public void requireOwner(Long userId, Authentication authentication) {
        if (authentication == null || !userId.equals(authentication.getPrincipal())) {
            throw new TechShopException("You do not have permission to access this resource", 403);
        }
    }

    public Long currentUserId(Authentication authentication) {
        return CurrentUser.id(authentication);
    }
}
