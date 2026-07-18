package am.techshop.wishlist.controller;

import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.WishlistItemResponse;
import am.techshop.common.dto.response.WishlistResponse;
import am.techshop.wishlist.config.JwtAuthFilter;
import am.techshop.wishlist.config.SecurityConfig;
import am.techshop.wishlist.service.JwtService;
import am.techshop.wishlist.service.WishlistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class WishlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WishlistService wishlistService;

    @MockBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;

    private Authentication authenticatedUser() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }

    @Test
    void addProduct_ReturnsCreatedWishlist() throws Exception {
        Long productId = 5L;
        ProductResponse product = new ProductResponse(productId, "Phone", "Desc", BigDecimal.valueOf(100), 10);
        WishlistItemResponse item = new WishlistItemResponse(1L, product, LocalDateTime.now());
        WishlistResponse response = new WishlistResponse(1L, USER_ID, List.of(item), LocalDateTime.now());

        when(wishlistService.addToWishlist(USER_ID, productId)).thenReturn(response);

        mockMvc.perform(post("/wishlist/products/{productId}", productId)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Product added to wishlist"))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.items[0].product.id").value(productId));
    }

    @Test
    void addProduct_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/wishlist/products/{productId}", 5L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addProduct_WithNonPositiveProductId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/wishlist/products/{productId}", 0)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWishlist_ReturnsWishlist() throws Exception {
        ProductResponse product = new ProductResponse(5L, "Phone", "Desc", BigDecimal.valueOf(100), 10);
        WishlistItemResponse item = new WishlistItemResponse(1L, product, LocalDateTime.now());
        WishlistResponse response = new WishlistResponse(1L, USER_ID, List.of(item), LocalDateTime.now());

        when(wishlistService.getWishlist(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/wishlist")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].product.name").value("Phone"));
    }

    @Test
    void getWishlistCount_ReturnsCount() throws Exception {
        when(wishlistService.getWishlistCount(USER_ID)).thenReturn(3L);

        mockMvc.perform(get("/wishlist/count")
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));
    }

    @Test
    void removeProduct_ReturnsUpdatedWishlist() throws Exception {
        Long productId = 5L;
        WishlistResponse response = new WishlistResponse(1L, USER_ID, List.of(), LocalDateTime.now());

        when(wishlistService.removeFromWishlist(USER_ID, productId)).thenReturn(response);

        mockMvc.perform(delete("/wishlist/products/{productId}", productId)
                        .with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product removed from wishlist"))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }
}
