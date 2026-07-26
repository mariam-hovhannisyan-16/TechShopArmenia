package am.techshop.cart.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithoutAuthorizationHeader_SkipsAuthentication() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithInvalidToken_SkipsAuthentication() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn("Bearer bad-token");
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithValidToken_SetsAuthenticationWithUserIdPrincipalAndRoleAuthority() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(jwtService);
        when(request.getHeader("Authorization")).thenReturn("Bearer good-token");
        when(jwtService.isTokenValid("good-token")).thenReturn(true);
        when(jwtService.extractUserId("good-token")).thenReturn(1L);
        when(jwtService.extractRole("good-token")).thenReturn("CUSTOMER");

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(1L, authentication.getPrincipal());
        assertTrue(authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
        verify(filterChain).doFilter(request, response);
    }
}
