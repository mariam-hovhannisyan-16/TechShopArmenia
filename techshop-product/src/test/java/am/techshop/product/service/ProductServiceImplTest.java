package am.techshop.product.service;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.PricePredictionResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.SurpriseBoxResponse;
import am.techshop.common.event.PriceDropEvent;
import am.techshop.common.exception.ProductNotFoundException;
import am.techshop.common.exception.TechShopException;
import am.techshop.product.client.WishlistClient;
import am.techshop.product.entity.PriceHistory;
import am.techshop.product.entity.Product;
import am.techshop.product.kafka.ProductEventProducer;
import am.techshop.product.mapper.ProductMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.repository.PriceHistoryRepository;
import am.techshop.product.repository.ProductRepository;
import am.techshop.product.service.PricePredictionService;
import am.techshop.product.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private PricePredictionService pricePredictionService;

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

    @Test
    void updatePrice_LogsHistoryRowBeforeApplyingTheChange() {
        Long id = 1L;
        Product product = new Product();
        product.setId(id);
        product.setName("Phone");
        product.setPrice(BigDecimal.valueOf(200));
        ProductResponse response = new ProductResponse(id, "Phone", "Desc", BigDecimal.valueOf(150), 10, "Phones", null, false, null);

        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(response);

        productService.updatePrice(id, BigDecimal.valueOf(150));

        InOrder inOrder = inOrder(priceHistoryRepository, productRepository);
        inOrder.verify(priceHistoryRepository).save(argThat(history ->
                history.getProductId().equals(id)
                        && history.getOldPrice().compareTo(BigDecimal.valueOf(200)) == 0
                        && history.getNewPrice().compareTo(BigDecimal.valueOf(150)) == 0
                        && history.getChangedAt() != null));
        inOrder.verify(productRepository).save(product);
    }

    @Test
    void getPricePrediction_WhenProductNotFound_ThrowsException() {
        Long id = 1L;
        when(productRepository.existsById(id)).thenReturn(false);

        assertThrows(ProductNotFoundException.class,
                () -> productService.getPricePrediction(id));

        verify(pricePredictionService, never()).getPrediction(anyLong(), any());
    }

    @Test
    void getPricePrediction_WhenLast90DaysHaveEnoughRecords_UsesThatWindow() {
        Long id = 1L;
        List<PriceHistory> recentHistory = List.of(
                new PriceHistory(1L, id, BigDecimal.valueOf(100), BigDecimal.valueOf(90), LocalDateTime.now().minusDays(10)),
                new PriceHistory(2L, id, BigDecimal.valueOf(90), BigDecimal.valueOf(80), LocalDateTime.now().minusDays(5)),
                new PriceHistory(3L, id, BigDecimal.valueOf(80), BigDecimal.valueOf(70), LocalDateTime.now().minusDays(1)));
        PricePredictionResponse expected = new PricePredictionResponse("Գինը հավանաբար կնվազի", null, LocalDateTime.now());

        when(productRepository.existsById(id)).thenReturn(true);
        when(priceHistoryRepository.findByProductIdAndChangedAtAfterOrderByChangedAtAsc(eq(id), any())).thenReturn(recentHistory);
        when(pricePredictionService.getPrediction(id, recentHistory)).thenReturn(expected);

        PricePredictionResponse result = productService.getPricePrediction(id);

        assertEquals(expected, result);
        verify(priceHistoryRepository, never()).findByProductIdOrderByChangedAtAsc(any());
    }

    @Test
    void getPricePrediction_WhenLast90DaysWindowTooSparse_FallsBackToFullHistory() {
        Long id = 1L;
        List<PriceHistory> sparseWindow = List.of(
                new PriceHistory(1L, id, BigDecimal.valueOf(100), BigDecimal.valueOf(90), LocalDateTime.now().minusDays(1)));
        List<PriceHistory> fullHistory = List.of(
                new PriceHistory(1L, id, BigDecimal.valueOf(120), BigDecimal.valueOf(110), LocalDateTime.now().minusDays(200)),
                new PriceHistory(2L, id, BigDecimal.valueOf(110), BigDecimal.valueOf(100), LocalDateTime.now().minusDays(100)),
                new PriceHistory(3L, id, BigDecimal.valueOf(100), BigDecimal.valueOf(90), LocalDateTime.now().minusDays(1)));
        PricePredictionResponse expected = new PricePredictionResponse("Գինը կայուն է", null, LocalDateTime.now());

        when(productRepository.existsById(id)).thenReturn(true);
        when(priceHistoryRepository.findByProductIdAndChangedAtAfterOrderByChangedAtAsc(eq(id), any())).thenReturn(sparseWindow);
        when(priceHistoryRepository.findByProductIdOrderByChangedAtAsc(id)).thenReturn(fullHistory);
        when(pricePredictionService.getPrediction(id, fullHistory)).thenReturn(expected);

        PricePredictionResponse result = productService.getPricePrediction(id);

        assertEquals(expected, result);
    }

    private Product product(long id, String category, double price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setCategory(category);
        product.setPrice(BigDecimal.valueOf(price));
        product.setStock(stock);
        return product;
    }

    private void stubGenericMapper() {
        when(productMapper.toResponse(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            return new ProductResponse(p.getId(), p.getName(), null, p.getPrice(), p.getStock(),
                    p.getCategory(), null, false, null);
        });
    }

    @Test
    void getSurpriseBox_WithReasonableBudget_ReturnsNonEmptyBoxWithinBudget() {
        List<Product> available = List.of(
                product(1, "Phones", 100, 5),
                product(2, "Laptops", 200, 3),
                product(3, "TVs", 150, 2),
                product(4, "Audio", 80, 10));
        when(productRepository.findByStockGreaterThan(0)).thenReturn(available);
        stubGenericMapper();

        SurpriseBoxResponse box = productService.getSurpriseBox(BigDecimal.valueOf(300));

        assertFalse(box.items().isEmpty());
        assertTrue(box.totalPrice().compareTo(BigDecimal.valueOf(300)) <= 0);
        assertEquals(box.totalPrice(), BigDecimal.valueOf(300).subtract(box.remainingBudget()));
    }

    @Test
    void getSurpriseBox_WithVeryLowBudget_ReturnsEmptyBoxGracefully() {
        List<Product> available = List.of(
                product(1, "Phones", 500, 5),
                product(2, "Laptops", 600, 3),
                product(3, "TVs", 700, 2));
        when(productRepository.findByStockGreaterThan(0)).thenReturn(available);

        SurpriseBoxResponse box = productService.getSurpriseBox(BigDecimal.valueOf(100));

        assertTrue(box.items().isEmpty());
        assertEquals(0, box.totalPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0, box.remainingBudget().compareTo(BigDecimal.valueOf(100)));
    }

    @Test
    void getSurpriseBox_WhenMultipleAffordableCategoriesExist_PicksOneFromEachBeforeDoublingUp() {
        List<Product> available = List.of(
                product(1, "Phones", 100, 5),
                product(2, "Laptops", 100, 5),
                product(3, "TVs", 100, 5));
        when(productRepository.findByStockGreaterThan(0)).thenReturn(available);
        stubGenericMapper();

        SurpriseBoxResponse box = productService.getSurpriseBox(BigDecimal.valueOf(300));

        assertEquals(3, box.items().size());
        assertEquals(3, box.items().stream().map(ProductResponse::category).distinct().count());
        assertEquals(0, box.remainingBudget().compareTo(BigDecimal.ZERO));
    }

    @Test
    void getSurpriseBox_WhenNoProductsInStock_ReturnsEmptyBox() {
        when(productRepository.findByStockGreaterThan(0)).thenReturn(List.of());

        SurpriseBoxResponse box = productService.getSurpriseBox(BigDecimal.valueOf(1000));

        assertTrue(box.items().isEmpty());
        assertEquals(0, box.remainingBudget().compareTo(BigDecimal.valueOf(1000)));
    }
}
