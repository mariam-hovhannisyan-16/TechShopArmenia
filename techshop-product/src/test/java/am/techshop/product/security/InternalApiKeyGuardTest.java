package am.techshop.product.security;

import am.techshop.common.exception.TechShopException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalApiKeyGuardTest {

    private final InternalApiKeyGuard guard = new InternalApiKeyGuard();

    @BeforeEach
    void configureKey() {
        ReflectionTestUtils.setField(guard, "internalApiKey", "correct-key");
    }

    @Test
    void verify_WhenKeyMatches_DoesNotThrow() {
        assertDoesNotThrow(() -> guard.verify("correct-key"));
    }

    @Test
    void verify_WhenKeyDoesNotMatch_ThrowsForbidden() {
        TechShopException ex = assertThrows(TechShopException.class, () -> guard.verify("wrong-key"));
        assertEquals(403, ex.getStatusCode());
    }

    @Test
    void verify_WhenKeyMissing_ThrowsForbidden() {
        TechShopException ex = assertThrows(TechShopException.class, () -> guard.verify(null));
        assertEquals(403, ex.getStatusCode());
    }
}
