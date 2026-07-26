package am.techshop.product.service;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.event.PriceDropEvent;
import am.techshop.common.exception.ProductNotFoundException;
import am.techshop.common.exception.TechShopException;
import am.techshop.product.client.WishlistClient;
import am.techshop.product.entity.Product;
import am.techshop.product.kafka.ProductEventProducer;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private WishlistClient wishlistClient;

    @Mock
    private ProductEventProducer productEventProducer;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void addProduct_SavesAndReturnsProduct() {
        ProductRequest request = new ProductRequest("Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false);
        Product newProduct = new Product();
        newProduct.setName("Phone");
        newProduct.setDescription("Desc");
        newProduct.setPrice(BigDecimal.valueOf(100));
        newProduct.setStock(10);
        newProduct.setCategory("Phones");
        newProduct.setImageUrl("img.jpg");
        Product savedProduct = new Product();
        savedProduct.setId(1L);
        savedProduct.setName("Phone");
        savedProduct.setDescription("Desc");
        savedProduct.setPrice(BigDecimal.valueOf(100));
        savedProduct.setStock(10);
        savedProduct.setCategory("Phones");
        savedProduct.setImageUrl("img.jpg");
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", "img.jpg", false, null);

        when(categoryRepository.existsByNameIgnoreCase("Phones")).thenReturn(true);
        when(productMapper.toEntity(request)).thenReturn(newProduct);
        when(productRepository.save(newProduct)).thenReturn(savedProduct);
        when(productMapper.toResponse(savedProduct)).thenReturn(response);

        ProductResponse result = productService.addProduct(request);

        assertNotNull(result);
        assertEquals("Phone", result.name());
        verify(productRepository).save(newProduct);
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
        Product product = new Product();
        product.setId(1L);
        product.setName("Phone");
        ProductResponse response = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        Page<Product> page = new PageImpl<>(List.of(product));

        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class))).thenReturn(page);
        when(productMapper.toResponse(product)).thenReturn(response);

        PageResponse<ProductResponse> result = productService.getAllProducts("Phones", "phone", 0, 20);

        assertEquals(1, result.content().size());
        assertEquals("Phone", result.content().getFirst().name());
        assertEquals(1, result.totalElements());
    }

    @Test
    void getProductById_WhenExists_ReturnsProduct() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
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
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setStock(10);
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
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setStock(1);
        when(productRepository.findById(id)).thenReturn(Optional.of(product));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> productService.adjustStock(id, -5));

        assertEquals(409, ex.getStatusCode());
        verify(productRepository, never()).save(any());
    }

    @Test
    void adjustStock_WhenRestoringStock_IncreasesStock() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setStock(3);
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
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(100));
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
    void updatePrice_WhenPriceDrops_NotifiesEachWishlistSubscriber() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(200));
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(150), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);
        when(wishlistClient.getSubscribers(eq(id), any()))
                .thenReturn(new ApiResponse<>(true, "Success", List.of(10L, 20L)));

        productService.updatePrice(id, BigDecimal.valueOf(150));

        verify(productEventProducer).sendPriceDropEvent(new PriceDropEvent(10L, id, "Phone", BigDecimal.valueOf(150)));
        verify(productEventProducer).sendPriceDropEvent(new PriceDropEvent(20L, id, "Phone", BigDecimal.valueOf(150)));
    }

    @Test
    void updatePrice_WhenPriceIncreases_DoesNotNotifySubscribers() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(100));
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(150), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.updatePrice(id, BigDecimal.valueOf(150));

        verify(wishlistClient, never()).getSubscribers(any(), any());
        verify(productEventProducer, never()).sendPriceDropEvent(any());
    }

    @Test
    void updatePrice_WhenPriceUnchanged_DoesNotNotifySubscribers() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(100));
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.updatePrice(id, BigDecimal.valueOf(100));

        verify(productEventProducer, never()).sendPriceDropEvent(any());
    }

    @Test
    void updatePrice_WhenNoWishlistSubscribers_DoesNotPublishAnyEvent() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(200));
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(150), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);
        when(wishlistClient.getSubscribers(eq(id), any()))
                .thenReturn(new ApiResponse<>(true, "Success", List.of()));

        productService.updatePrice(id, BigDecimal.valueOf(150));

        verify(productEventProducer, never()).sendPriceDropEvent(any());
    }

    @Test
    void updatePrice_WhenWishlistServiceUnavailable_StillUpdatesPrice() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(200));
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(150), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);
        when(wishlistClient.getSubscribers(eq(id), any())).thenThrow(new RuntimeException("wishlist-service unavailable"));

        ProductResponse result = productService.updatePrice(id, BigDecimal.valueOf(150));

        assertEquals(BigDecimal.valueOf(150), result.price());
        verify(productEventProducer, never()).sendPriceDropEvent(any());
    }

    @Test
    void updateDiscount_WhenExists_SetsDiscount() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
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
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setDiscountPercentage(20);
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
