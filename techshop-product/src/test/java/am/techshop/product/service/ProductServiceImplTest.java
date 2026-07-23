package am.techshop.product.service;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.exception.ProductNotFoundException;
import am.techshop.common.exception.TechShopException;
import am.techshop.product.entity.Product;
import am.techshop.product.mapper.ProductMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.repository.ProductRepository;
import am.techshop.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void addProduct_SavesAndReturnsProduct() {
        ProductRequest request = new ProductRequest("Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false);
        Product product = Product.builder()
                .id(1L)
                .name("Phone")
                .description("Desc")
                .price(BigDecimal.valueOf(100))
                .stock(10)
                .category("Phones")
                .imageUrl("img.jpg")
                .build();
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false, null);

        when(categoryRepository.existsByNameIgnoreCase("Phones")).thenReturn(true);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.addProduct(request);

        assertNotNull(result);
        assertEquals("Phone", result.name());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void addProduct_WhenCategoryUnknown_ThrowsBadRequest() {
        ProductRequest request = new ProductRequest("Phone", "Desc", BigDecimal.valueOf(100), 10, "Unknown", null, false);

        when(categoryRepository.existsByNameIgnoreCase("Unknown")).thenReturn(false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> productService.addProduct(request));

        assertEquals(400, ex.getStatusCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    void getAllProducts_ReturnsPagedProductList() {
        Product product = Product.builder().id(1L).name("Phone").build();
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        PageResponse<ProductResponse> result = productService.getAllProducts("Phones", "phone", 0, 20);

        assertEquals(1, result.content().size());
        assertEquals("Phone", result.content().get(0).name());
        assertEquals(1, result.totalElements());
    }

    @Test
    void getProductById_WhenExists_ReturnsProduct() {
        Long id = 1L;
        Product product = Product.builder().id(id).name("Phone").build();
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.getProductById(id);

        assertNotNull(result);
        assertEquals(id, result.id());
    }

    @Test
    void getProductById_WhenNotFound_ThrowsException() {
        Long id = 1L;
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.getProductById(id));
    }

    @Test
    void deleteProduct_WhenExists_DeletesProduct() {
        Long id = 1L;
        when(productRepository.existsById(id)).thenReturn(true);

        productService.deleteProduct(id);

        verify(productRepository).deleteById(id);
    }

    @Test
    void deleteProduct_WhenNotFound_ThrowsException() {
        Long id = 1L;
        when(productRepository.existsById(id)).thenReturn(false);

        assertThrows(ProductNotFoundException.class,
                () -> productService.deleteProduct(id));
    }

    @Test
    void adjustStock_WhenSufficientStock_ReducesStock() {
        Long id = 1L;
        Product product = Product.builder().id(id).name("Phone").stock(10).build();
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 8, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.adjustStock(id, -2);

        assertEquals(8, product.getStock());
        assertEquals(8, result.stock());
    }

    @Test
    void adjustStock_WhenInsufficientStock_ThrowsConflict() {
        Long id = 1L;
        Product product = Product.builder().id(id).name("Phone").stock(1).build();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> productService.adjustStock(id, -5));

        assertEquals(409, ex.getStatusCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    void adjustStock_WhenRestoringStock_IncreasesStock() {
        Long id = 1L;
        Product product = Product.builder().id(id).name("Phone").stock(3).build();
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 5, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.adjustStock(id, 2);

        assertEquals(5, product.getStock());
        assertEquals(5, result.stock());
    }

    @Test
    void updatePrice_WhenExists_UpdatesPrice() {
        Long id = 1L;
        Product product = Product.builder().id(id).name("Phone").price(BigDecimal.valueOf(100)).build();
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(150), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.updatePrice(id, BigDecimal.valueOf(150));

        assertEquals(BigDecimal.valueOf(150), product.getPrice());
        assertEquals(BigDecimal.valueOf(150), result.price());
    }

    @Test
    void updatePrice_WhenNotFound_ThrowsException() {
        Long id = 1L;
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updatePrice(id, BigDecimal.valueOf(150)));
    }

    @Test
    void updateDiscount_WhenExists_SetsDiscount() {
        Long id = 1L;
        Product product = Product.builder().id(id).name("Phone").build();
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, 20);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.updateDiscount(id, 20);

        assertEquals(20, product.getDiscountPercentage());
        assertEquals(20, result.discountPercentage());
    }

    @Test
    void updateDiscount_WithNull_ClearsDiscount() {
        Long id = 1L;
        Product product = Product.builder().id(id).name("Phone").discountPercentage(20).build();
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        ProductResponse result = productService.updateDiscount(id, null);

        assertNull(product.getDiscountPercentage());
        assertNull(result.discountPercentage());
    }

    @Test
    void updateDiscount_WhenNotFound_ThrowsException() {
        Long id = 1L;
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class,
                () -> productService.updateDiscount(id, 20));
    }
}
