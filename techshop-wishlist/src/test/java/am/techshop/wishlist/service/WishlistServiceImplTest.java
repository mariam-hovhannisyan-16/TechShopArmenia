package am.techshop.wishlist.service;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.WishlistItemResponse;
import am.techshop.common.dto.response.WishlistResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.wishlist.client.ProductClient;
import am.techshop.wishlist.entity.Wishlist;
import am.techshop.wishlist.entity.WishlistItem;
import am.techshop.wishlist.mapper.WishlistMapper;
import am.techshop.wishlist.repository.WishlistRepository;
import am.techshop.wishlist.service.impl.WishlistServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

    @Mock
    private WishlistRepository wishlistRepository;

    @Mock
    private WishlistMapper wishlistMapper;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private WishlistServiceImpl wishlistService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 5L;

    @Test
    void addToWishlist_WhenWishlistExistsAndProductIsNew_AddsItem() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        ProductResponse product = new ProductResponse(PRODUCT_ID, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        WishlistItem newItem = new WishlistItem();
        newItem.setProductId(PRODUCT_ID);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(new ApiResponse<>(true, "Success", product));
        when(wishlistMapper.toItem(PRODUCT_ID)).thenReturn(newItem);
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(wishlistMapper.toResponse(eq(wishlist), any()))
                .thenReturn(new WishlistResponse(1L, USER_ID, List.of(), null));

        WishlistResponse result = wishlistService.addToWishlist(USER_ID, PRODUCT_ID);

        assertNotNull(result);
        assertEquals(1, wishlist.getItems().size());
        assertEquals(PRODUCT_ID, wishlist.getItems().getFirst().getProductId());
        verify(wishlistRepository).save(wishlist);
    }

    @Test
    void addToWishlist_WhenWishlistDoesNotExist_CreatesItFirst() {
        Wishlist newWishlist = new Wishlist();
        newWishlist.setUserId(USER_ID);
        WishlistItem newItem = new WishlistItem();
        newItem.setProductId(PRODUCT_ID);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(wishlistMapper.toEntity(USER_ID)).thenReturn(newWishlist);
        when(wishlistMapper.toItem(PRODUCT_ID)).thenReturn(newItem);
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse product = new ProductResponse(PRODUCT_ID, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(new ApiResponse<>(true, "Success", product));
        when(wishlistMapper.toResponse(any(), any()))
                .thenReturn(new WishlistResponse(null, USER_ID, List.of(), null));

        WishlistResponse result = wishlistService.addToWishlist(USER_ID, PRODUCT_ID);

        assertNotNull(result);
        verify(wishlistRepository, times(2)).save(any(Wishlist.class));
    }

    @Test
    void addToWishlist_WhenWishlistDoesNotExistAndConcurrentInsertRaces_ThrowsConflict() {
        WishlistItem newItem = new WishlistItem();
        newItem.setProductId(PRODUCT_ID);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(wishlistMapper.toEntity(USER_ID)).thenReturn(new Wishlist());
        when(wishlistRepository.save(any(Wishlist.class)))
                .thenThrow(new DataIntegrityViolationException("uk_wishlist_user"));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.addToWishlist(USER_ID, PRODUCT_ID));

        assertEquals(409, ex.getStatusCode());
        verify(productClient, never()).getProduct(any());
    }

    @Test
    void addToWishlist_WhenAlreadyExists_ThrowsConflict() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        WishlistItem item = new WishlistItem();
        item.setProductId(PRODUCT_ID);
        wishlist.addItem(item);
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.addToWishlist(USER_ID, PRODUCT_ID));

        assertEquals(409, ex.getStatusCode());
        verify(wishlistRepository, never()).save(any());
        verify(productClient, never()).getProduct(any());
    }

    @Test
    void addToWishlist_WhenProductNotFound_ThrowsNotFound() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(new ApiResponse<>(false, "Not found", null));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.addToWishlist(USER_ID, PRODUCT_ID));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void getWishlist_WhenExists_ReturnsMappedWishlist() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        WishlistItem item = new WishlistItem();
        item.setId(1L);
        item.setProductId(PRODUCT_ID);
        wishlist.addItem(item);
        ProductResponse product = new ProductResponse(PRODUCT_ID, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(productClient.getProducts(List.of(PRODUCT_ID))).thenReturn(new ApiResponse<>(true, "Success", List.of(product)));
        when(wishlistMapper.toResponse(eq(wishlist), any()))
                .thenReturn(new WishlistResponse(1L, USER_ID, List.of(), wishlist.getCreatedAt()));

        WishlistResponse result = wishlistService.getWishlist(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
    }

    @Test
    void getWishlist_WhenProductMissingFromBatchResponse_ReturnsNullProductForThatItem() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        WishlistItem item = new WishlistItem();
        item.setId(1L);
        item.setProductId(PRODUCT_ID);
        wishlist.addItem(item);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(productClient.getProducts(List.of(PRODUCT_ID))).thenReturn(new ApiResponse<>(true, "Success", List.of()));
        when(wishlistMapper.toItemResponse(item, null))
                .thenReturn(new WishlistItemResponse(1L, null, null));
        when(wishlistMapper.toResponse(eq(wishlist), any()))
                .thenReturn(new WishlistResponse(1L, USER_ID, List.of(), wishlist.getCreatedAt()));

        WishlistResponse result = wishlistService.getWishlist(USER_ID);

        assertNotNull(result);
        verify(wishlistMapper).toItemResponse(item, null);
    }

    @Test
    void getWishlist_WhenProductServiceUnavailable_ReturnsNullProductsWithoutThrowing() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        WishlistItem item = new WishlistItem();
        item.setId(1L);
        item.setProductId(PRODUCT_ID);
        wishlist.addItem(item);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        Request request = Request.create(Request.HttpMethod.GET, "/api/products/batch",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(503).reason("Unavailable").request(request).build();
        when(productClient.getProducts(List.of(PRODUCT_ID)))
                .thenThrow(FeignException.errorStatus("ProductClient#getProducts", response));
        when(wishlistMapper.toItemResponse(item, null))
                .thenReturn(new WishlistItemResponse(1L, null, null));
        when(wishlistMapper.toResponse(eq(wishlist), any()))
                .thenReturn(new WishlistResponse(1L, USER_ID, List.of(), wishlist.getCreatedAt()));

        WishlistResponse result = wishlistService.getWishlist(USER_ID);

        assertNotNull(result);
        verify(wishlistMapper).toItemResponse(item, null);
    }

    @Test
    void getWishlist_WhenNotExists_ReturnsEmptyWishlist() {
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        WishlistResponse result = wishlistService.getWishlist(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void getSubscriberUserIds_ReturnsUserIdsFromRepository() {
        when(wishlistRepository.findUserIdsByProductId(PRODUCT_ID)).thenReturn(List.of(1L, 2L, 3L));

        List<Long> result = wishlistService.getSubscriberUserIds(PRODUCT_ID);

        assertEquals(List.of(1L, 2L, 3L), result);
    }

    @Test
    void getWishlistCount_ReturnsCount() {
        when(wishlistRepository.countItemsByUserId(USER_ID)).thenReturn(3L);

        long count = wishlistService.getWishlistCount(USER_ID);

        assertEquals(3L, count);
    }

    @Test
    void removeFromWishlist_WhenExists_RemovesItem() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        WishlistItem item = new WishlistItem();
        item.setProductId(PRODUCT_ID);
        wishlist.addItem(item);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(wishlistMapper.toResponse(eq(wishlist), any()))
                .thenReturn(new WishlistResponse(1L, USER_ID, List.of(), null));

        WishlistResponse result = wishlistService.removeFromWishlist(USER_ID, PRODUCT_ID);

        assertNotNull(result);
        assertTrue(wishlist.getItems().isEmpty());
        verify(wishlistRepository).save(wishlist);
    }

    @Test
    void removeFromWishlist_WhenWishlistNotFound_ThrowsException() {
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.removeFromWishlist(USER_ID, PRODUCT_ID));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void removeFromWishlist_WhenProductNotInWishlist_ThrowsException() {
        Wishlist wishlist = new Wishlist();
        wishlist.setId(1L);
        wishlist.setUserId(USER_ID);
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.removeFromWishlist(USER_ID, PRODUCT_ID));

        assertEquals(404, ex.getStatusCode());
    }
}
