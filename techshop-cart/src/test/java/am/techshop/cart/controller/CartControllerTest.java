package am.techshop.cart.controller;

import am.techshop.cart.service.CartService;
import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.CartResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @Test
    void getCart_ReturnsCart() throws Exception {
        Long userId = 1L;
        CartResponse cartResponse = new CartResponse(1L, userId, new ArrayList<>(), BigDecimal.ZERO);

        when(cartService.getCart(userId)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/cart/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId));
    }

    @Test
    void addItem_ReturnsUpdatedCart() throws Exception {
        Long userId = 1L;
        AddItemRequest request = new AddItemRequest(1L, 2);
        CartResponse cartResponse = new CartResponse(1L, userId, new ArrayList<>(), BigDecimal.valueOf(200));

        when(cartService.addItem(eq(userId), any(AddItemRequest.class))).thenReturn(cartResponse);

        mockMvc.perform(post("/api/cart/{userId}/items", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added"));
    }

    @Test
    void removeItem_ReturnsUpdatedCart() throws Exception {
        Long userId = 1L;
        Long productId = 1L;
        CartResponse cartResponse = new CartResponse(1L, userId, new ArrayList<>(), BigDecimal.ZERO);

        when(cartService.removeItem(userId, productId)).thenReturn(cartResponse);

        mockMvc.perform(delete("/api/cart/{userId}/items/{productId}", userId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item removed"));
    }

    @Test
    void clearCart_ReturnsSuccess() throws Exception {
        Long userId = 1L;
        doNothing().when(cartService).clearCart(userId);

        mockMvc.perform(delete("/api/cart/{userId}/clear", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared"));
    }
}