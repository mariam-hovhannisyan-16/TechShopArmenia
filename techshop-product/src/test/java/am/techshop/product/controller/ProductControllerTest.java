package am.techshop.product.controller;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.request.StockAdjustmentRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void addProduct_ReturnsCreatedProduct() throws Exception {
        ProductRequest request = new ProductRequest("Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false);
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false);

        when(productService.addProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Product created"));
    }

    @Test
    void getAllProducts_ReturnsPagedProductList() throws Exception {
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false);
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
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false);

        when(productService.getProductById(id)).thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
    }

    @Test
    void deleteProduct_ReturnsSuccess() throws Exception {
        Long id = 1L;
        doNothing().when(productService).deleteProduct(id);

        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Product deleted"));
    }

    @Test
    void adjustStock_WithCorrectInternalApiKey_ReturnsUpdatedProduct() throws Exception {
        Long id = 1L;
        StockAdjustmentRequest request = new StockAdjustmentRequest(-2);
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 8, "Phones", null, false);

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
}
