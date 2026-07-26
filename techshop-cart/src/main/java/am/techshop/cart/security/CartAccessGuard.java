package am.techshop.cart.security;

import am.techshop.common.exception.TechShopException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class CartAccessGuard {

    @Value("${internal.api-key}")
    private String internalApiKey;

    public void verifyAccess(Long userId, Authentication authentication, String providedApiKey) {
        if (providedApiKey != null && MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                providedApiKey.getBytes(StandardCharsets.UTF_8))) {
            return;
        }
        if (authentication == null) {
            throw new TechShopException("Authentication or X-Internal-Api-Key header is required", 401);
        }
        if (!userId.equals(authentication.getPrincipal())) {
            throw new TechShopException("You do not have permission to access this cart", 403);
        }
        boolean isCustomer = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_CUSTOMER"::equals);
        if (!isCustomer) {
            throw new TechShopException("Only customer accounts can access the cart", 403);
        }
    }
}
