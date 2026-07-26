package am.techshop.wishlist.security;

import am.techshop.common.exception.TechShopException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiKeyGuard {

    @Value("${internal.api-key}")
    private String internalApiKey;

    public void verify(String providedKey) {
        if (providedKey == null || !MessageDigest.isEqual(
                internalApiKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new TechShopException("Forbidden", 403);
        }
    }
}
