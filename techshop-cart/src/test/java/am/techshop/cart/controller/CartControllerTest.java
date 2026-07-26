package am.techshop.cart.controller;

import am.techshop.cart.config.SecurityConfig;
import am.techshop.cart.security.CartAccessGuard;
import am.techshop.cart.security.JwtAuthFilter;
import am.techshop.cart.security.JwtService;
import am.techshop.cart.service.CartService;
import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.CartResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, CartAccessGuard.class})
class CartControllerTest {

    private static final String INTERNAL_KEY = "test-internal-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;

    private Authentication asUser(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin(Long userId) {
        return new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void getCart_AsOwner_ReturnsCart() throws Exception {
        CartResponse cartResponse = new CartResponse(1L, USER_ID, new ArrayList<>(), BigDecimal.ZERO);

        when(cartService.getCart(USER_ID)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart/{userId}", USER_ID).with(authentication(asUser(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(USER_ID));
    }

    @Test
    void getCart_WithInternalApiKey_ReturnsCart() throws Exception {
        CartResponse cartResponse = new CartResponse(1L, USER_ID, new ArrayList<>(), BigDecimal.ZERO);

        when(cartService.getCart(USER_ID)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart/{userId}", USER_ID).header("X-Internal-Api-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(USER_ID));
    }

    @Test
    void getCart_AsDifferentUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/cart/{userId}", USER_ID).with(authentication(asUser(2L))))
                .andExpect(status().isForbidden());

        verify(cartService, never()).getCart(any());
    }

    @Test
    void getCart_AsAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/cart/{userId}", USER_ID).with(authentication(asAdmin(USER_ID))))
                .andExpect(status().isForbidden());

        verify(cartService, never()).getCart(any());
    }

    @Test
    void getCart_WithoutAuthOrKey_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart/{userId}", USER_ID))
                .andExpect(status().isUnauthorized());

        verify(cartService, never()).getCart(any());
    }

    @Test
    void addItem_AsOwner_ReturnsUpdatedCart() throws Exception {
        AddItemRequest request = new AddItemRequest(1L, 2);
        CartResponse cartResponse = new CartResponse(1L, USER_ID, new ArrayList<>(), BigDecimal.valueOf(200));

        when(cartService.addItem(eq(USER_ID), any(AddItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/{userId}/items", USER_ID)
                        .with(authentication(asUser(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added"));
    }

    @Test
    void addItem_AsDifferentUser_ReturnsForbidden() throws Exception {
        AddItemRequest request = new AddItemRequest(1L, 2);

        mockMvc.perform(post("/api/cart/{userId}/items", USER_ID)
                        .with(authentication(asUser(2L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(cartService, never()).addItem(any(), any());
    }

    @Test
    void addItem_AsAdmin_ReturnsForbidden() throws Exception {
        AddItemRequest request = new AddItemRequest(1L, 2);

        mockMvc.perform(post("/api/cart/{userId}/items", USER_ID)
                        .with(authentication(asAdmin(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(cartService, never()).addItem(any(), any());
    }

    @Test
    void removeItem_AsOwner_ReturnsUpdatedCart() throws Exception {
        Long productId = 1L;
        CartResponse cartResponse = new CartResponse(1L, USER_ID, new ArrayList<>(), BigDecimal.ZERO);

        when(cartService.removeItem(USER_ID, productId)).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/{userId}/items/{productId}", USER_ID, productId)
                        .with(authentication(asUser(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item removed"));
    }

    @Test
    void removeItem_AsDifferentUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/cart/{userId}/items/{productId}", USER_ID, 1L)
                        .with(authentication(asUser(2L))))
                .andExpect(status().isForbidden());

        verify(cartService, never()).removeItem(any(), any());
    }

    @Test
    void removeItem_AsAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/cart/{userId}/items/{productId}", USER_ID, 1L)
                        .with(authentication(asAdmin(USER_ID))))
                .andExpect(status().isForbidden());

        verify(cartService, never()).removeItem(any(), any());
    }

    @Test
    void clearCart_AsOwner_ReturnsSuccess() throws Exception {
        doNothing().when(cartService).clearCart(USER_ID);

        mockMvc.perform(delete("/api/cart/{userId}/clear", USER_ID).with(authentication(asUser(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared"));
    }

    @Test
    void clearCart_WithInternalApiKey_ReturnsSuccess() throws Exception {
        doNothing().when(cartService).clearCart(USER_ID);

        mockMvc.perform(delete("/api/cart/{userId}/clear", USER_ID).header("X-Internal-Api-Key", INTERNAL_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared"));
    }

    @Test
    void clearCart_AsDifferentUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/cart/{userId}/clear", USER_ID).with(authentication(asUser(2L))))
                .andExpect(status().isForbidden());

        verify(cartService, never()).clearCart(any());
    }

    @Test
    void clearCart_AsAdmin_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/cart/{userId}/clear", USER_ID).with(authentication(asAdmin(USER_ID))))
                .andExpect(status().isForbidden());

        verify(cartService, never()).clearCart(any());
    }
}
