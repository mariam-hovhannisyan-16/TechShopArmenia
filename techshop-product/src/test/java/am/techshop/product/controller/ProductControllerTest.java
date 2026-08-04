package am.techshop.product.controller;

import am.techshop.common.dto.request.DiscountUpdateRequest;
import am.techshop.common.dto.request.PriceUpdateRequest;
import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.request.StockAdjustmentRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.PricePredictionResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.SurpriseBoxResponse;
import am.techshop.common.exception.ProductNotFoundException;
import am.techshop.product.config.SecurityConfig;
import am.techshop.product.security.InternalApiKeyGuard;
import am.techshop.product.security.JwtAuthFilter;
import am.techshop.product.security.JwtService;
import am.techshop.product.service.ProductService;
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
import java.time.LocalDateTime;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, InternalApiKeyGuard.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void addProduct_WhenCalledByAdmin_ReturnsCreatedProduct() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false);
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false, null);

        when(productService.addProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Product created"));
    }

    @Test
    void addProduct_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false);

        mockMvc.perform(post("/api/products")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productService, never()).addProduct(any());
    }

    @Test
    void addProduct_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).addProduct(any());
    }

    @Test
    void getAllProducts_ReturnsPagedProductList() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(response), 0, 20, 1, 1);

        when(productService.getAllProducts(any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Phone"));
    }

    @Test
    void getAllProducts_WithCategoryAndSearch_PassesFiltersThrough() throws Exception {
        PageResponse<ProductResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0);

        when(productService.getAllProducts("Phones", "iphone", 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/products").param("category", "Phones").param("search", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getProductById_ReturnsProduct() throws Exception {
        Long id = 1L;
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(productService.getProductById(id)).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void deleteProduct_WhenCalledByAdmin_ReturnsSuccess() throws Exception {
        Long id = 1L;
        doNothing().when(productService).deleteProduct(id);

        mockMvc.perform(delete("/api/products/{id}", id).with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted"));
    }

    @Test
    void deleteProduct_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", 1L).with(authentication(asUser())))
                .andExpect(status().isForbidden());

        verify(productService, never()).deleteProduct(any());
    }

    @Test
    void deleteProduct_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", 1L))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).deleteProduct(any());
    }

    @Test
    void adjustStock_WithCorrectInternalApiKey_ReturnsUpdatedProduct() throws Exception {
        Long id = 1L;
        StockAdjustmentRequest request = new StockAdjustmentRequest(-2);
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 8, "Phones", null, false, null);

        when(productService.adjustStock(id, -2)).thenReturn(response);

        mockMvc.perform(patch("/api/products/{id}/stock", id)
                        .header("X-Internal-Api-Key", "local-dev-internal-key-change-in-production")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stock").value(8));
    }

    @Test
    void adjustStock_WithWrongInternalApiKey_ReturnsForbidden() throws Exception {
        StockAdjustmentRequest request = new StockAdjustmentRequest(-2);

        mockMvc.perform(patch("/api/products/{id}/stock", 1L)
                        .header("X-Internal-Api-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productService, never()).adjustStock(any(), anyInt());
    }

    @Test
    void updatePrice_WhenCalledByAdmin_ReturnsUpdatedProduct() throws Exception {
        Long id = 1L;
        PriceUpdateRequest request = new PriceUpdateRequest(BigDecimal.valueOf(150));
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(150), 10, "Phones", null, false, null);

        when(productService.updatePrice(id, BigDecimal.valueOf(150))).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}/price", id)
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price").value(150));
    }

    @Test
    void updatePrice_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        PriceUpdateRequest request = new PriceUpdateRequest(BigDecimal.valueOf(150));

        mockMvc.perform(put("/api/products/{id}/price", 1L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productService, never()).updatePrice(any(), any());
    }

    @Test
    void updatePrice_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        PriceUpdateRequest request = new PriceUpdateRequest(BigDecimal.valueOf(150));

        mockMvc.perform(put("/api/products/{id}/price", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).updatePrice(any(), any());
    }

    @Test
    void updateDiscount_WhenCalledByAdmin_ReturnsUpdatedProduct() throws Exception {
        Long id = 1L;
        DiscountUpdateRequest request = new DiscountUpdateRequest(20);
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, 20);

        when(productService.updateDiscount(id, 20)).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}/discount", id)
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountPercentage").value(20));
    }

    @Test
    void updateDiscount_WithNullClearsDiscount() throws Exception {
        Long id = 1L;
        DiscountUpdateRequest request = new DiscountUpdateRequest(null);
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(productService.updateDiscount(id, null)).thenReturn(response);

        mockMvc.perform(put("/api/products/{id}/discount", id)
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.discountPercentage").doesNotExist());
    }

    @Test
    void updateDiscount_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        DiscountUpdateRequest request = new DiscountUpdateRequest(20);

        mockMvc.perform(put("/api/products/{id}/discount", 1L)
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(productService, never()).updateDiscount(any(), any());
    }

    @Test
    void getPricePrediction_WithEnoughHistory_ReturnsPrediction() throws Exception {
        Long id = 1L;
        PricePredictionResponse response = new PricePredictionResponse(
                "Գինը հավանաբար կնվազի ~5%-ով հաջորդ շաբաթ", null, LocalDateTime.now());

        when(productService.getPricePrediction(id)).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}/price-prediction", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prediction").value(response.prediction()))
                .andExpect(jsonPath("$.data.reason").doesNotExist());
    }

    @Test
    void getPricePrediction_WithInsufficientHistory_ReturnsReasonWithNullPrediction() throws Exception {
        Long id = 1L;
        PricePredictionResponse response = new PricePredictionResponse(null, "insufficient_history", null);

        when(productService.getPricePrediction(id)).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}/price-prediction", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prediction").doesNotExist())
                .andExpect(jsonPath("$.data.reason").value("insufficient_history"));
    }

    @Test
    void getPricePrediction_WhenProductNotFound_ReturnsNotFound() throws Exception {
        Long id = 1L;
        when(productService.getPricePrediction(id)).thenThrow(new ProductNotFoundException(id));

        mockMvc.perform(get("/api/products/{id}/price-prediction", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSurpriseBox_WithValidBudget_ReturnsBox() throws Exception {
        ProductResponse item = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        SurpriseBoxResponse box = new SurpriseBoxResponse(List.of(item), BigDecimal.valueOf(100), BigDecimal.valueOf(50));

        when(productService.getSurpriseBox(new BigDecimal("150"))).thenReturn(box);

        mockMvc.perform(get("/api/products/surprise-box").param("budget", "150"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(1))
                .andExpect(jsonPath("$.data.totalPrice").value(100))
                .andExpect(jsonPath("$.data.remainingBudget").value(50));
    }

    @Test
    void getSurpriseBox_WhenNothingFits_ReturnsEmptyBoxWithClearMessage() throws Exception {
        SurpriseBoxResponse box = new SurpriseBoxResponse(List.of(), BigDecimal.ZERO, new BigDecimal("100"));
        when(productService.getSurpriseBox(new BigDecimal("100"))).thenReturn(box);

        mockMvc.perform(get("/api/products/surprise-box").param("budget", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No products fit within this budget"))
                .andExpect(jsonPath("$.data.items").isEmpty());
    }

    @Test
    void getSurpriseBox_WhenBudgetMissing_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/surprise-box"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getSurpriseBox(any());
    }

    @Test
    void getSurpriseBox_WhenBudgetNotNumeric_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/surprise-box").param("budget", "not-a-number"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getSurpriseBox(any());
    }

    @Test
    void getSurpriseBox_WhenBudgetZeroOrNegative_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/products/surprise-box").param("budget", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/products/surprise-box").param("budget", "-50"))
                .andExpect(status().isBadRequest());

        verify(productService, never()).getSurpriseBox(any());
    }
}
