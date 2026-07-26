package am.techshop.product.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "test-only-jwt-secret-for-unit-tests-must-be-long-enough-1234";

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void configureService() {
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String tokenFor(Long userId, String role, long expiresInMs) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiresInMs))
                .signWith(key())
                .compact();
    }

    @Test
    void isTokenValid_WhenTokenWellFormedAndUnexpired_ReturnsTrue() {
        String token = tokenFor(1L, "ADMIN", 60_000);

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractUserId_ReturnsUserIdClaim() {
        String token = tokenFor(42L, "CUSTOMER", 60_000);

        assertEquals(42L, jwtService.extractUserId(token));
    }

    @Test
    void extractRole_ReturnsRoleClaim() {
        String token = tokenFor(1L, "ADMIN", 60_000);

        assertEquals("ADMIN", jwtService.extractRole(token));
    }

    @Test
    void isTokenValid_WhenTokenMalformed_ReturnsFalse() {
        assertFalse(jwtService.isTokenValid("not-a-real-jwt"));
    }

    @Test
    void isTokenValid_WhenSignedWithDifferentSecret_ReturnsFalse() {
        SecretKey otherKey = Keys.hmacShaKeyFor("a-completely-different-secret-value-used-only-here-12345".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .claim("userId", 1L)
                .claim("role", "CUSTOMER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        assertFalse(jwtService.isTokenValid(token));
    }

    @Test
    void isTokenValid_WhenTokenExpired_ReturnsFalse() {
        String expiredToken = tokenFor(1L, "CUSTOMER", -60_000);

        assertFalse(jwtService.isTokenValid(expiredToken));
    }
}
