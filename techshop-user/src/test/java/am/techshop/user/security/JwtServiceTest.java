package am.techshop.user.security;

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
        ReflectionTestUtils.setField(jwtService, "expirationMs", 86_400_000L);
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void generateToken_ProducesTokenThatIsValid() {
        String token = jwtService.generateToken(1L, "mariam@test.com", "CUSTOMER");

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractEmail_ReturnsEmailUsedToGenerateToken() {
        String token = jwtService.generateToken(1L, "mariam@test.com", "CUSTOMER");

        assertEquals("mariam@test.com", jwtService.extractEmail(token));
    }

    @Test
    void extractRole_ReturnsRoleUsedToGenerateToken() {
        String token = jwtService.generateToken(1L, "mariam@test.com", "ADMIN");

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
                .subject("mariam@test.com")
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
        String expiredToken = Jwts.builder()
                .subject("mariam@test.com")
                .claim("userId", 1L)
                .claim("role", "CUSTOMER")
                .issuedAt(new Date(System.currentTimeMillis() - 120_000))
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key())
                .compact();

        assertFalse(jwtService.isTokenValid(expiredToken));
    }
}
