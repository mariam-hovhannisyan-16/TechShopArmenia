package am.techshop.wishlist.service;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.WishlistResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.wishlist.client.ProductClient;
import am.techshop.wishlist.entity.Wishlist;
import am.techshop.wishlist.entity.WishlistItem;
import am.techshop.wishlist.mapper.WishlistMapper;
import am.techshop.wishlist.repository.WishlistRepository;
import am.techshop.wishlist.service.impl.WishlistServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
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
        Wishlist wishlist = Wishlist.builder().id(1L).userId(USER_ID).build();
        ProductResponse product = new ProductResponse(PRODUCT_ID, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(new ApiResponse<>(true, "Success", product));
        when(wishlistRepository.save(any(Wishlist.class))).thenAnswer(inv -> inv.getArgument(0));
        when(wishlistMapper.toResponse(eq(wishlist), any()))
                .thenReturn(new WishlistResponse(1L, USER_ID, List.of(), null));

        WishlistResponse result = wishlistService.addToWishlist(USER_ID, PRODUCT_ID);

        assertNotNull(result);
        assertEquals(1, wishlist.getItems().size());
        assertEquals(PRODUCT_ID, wishlist.getItems().get(0).getProductId());
        verify(wishlistRepository).save(wishlist);
    }

    @Test
    void addToWishlist_WhenWishlistDoesNotExist_CreatesItFirst() {
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
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
    void addToWishlist_WhenAlreadyExists_ThrowsConflict() {
        Wishlist wishlist = Wishlist.builder().id(1L).userId(USER_ID).build();
        wishlist.addItem(WishlistItem.builder().productId(PRODUCT_ID).build());
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.addToWishlist(USER_ID, PRODUCT_ID));

        assertEquals(409, ex.getStatusCode());
        verify(wishlistRepository, never()).save(any());
        verify(productClient, never()).getProduct(any());
    }

    @Test
    void addToWishlist_WhenProductNotFound_ThrowsNotFound() {
        Wishlist wishlist = Wishlist.builder().id(1L).userId(USER_ID).build();
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(new ApiResponse<>(false, "Not found", null));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.addToWishlist(USER_ID, PRODUCT_ID));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void getWishlist_WhenExists_ReturnsMappedWishlist() {
        Wishlist wishlist = Wishlist.builder().id(1L).userId(USER_ID).build();
        wishlist.addItem(WishlistItem.builder().id(1L).productId(PRODUCT_ID).build());
        ProductResponse product = new ProductResponse(PRODUCT_ID, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);

        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));
        when(productClient.getProduct(PRODUCT_ID)).thenReturn(new ApiResponse<>(true, "Success", product));
        when(wishlistMapper.toResponse(eq(wishlist), any()))
                .thenReturn(new WishlistResponse(1L, USER_ID, List.of(), wishlist.getCreatedAt()));

        WishlistResponse result = wishlistService.getWishlist(USER_ID);

        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
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
    void getWishlistCount_ReturnsCount() {
        when(wishlistRepository.countItemsByUserId(USER_ID)).thenReturn(3L);

        long count = wishlistService.getWishlistCount(USER_ID);

        assertEquals(3L, count);
    }

    @Test
    void removeFromWishlist_WhenExists_RemovesItem() {
        Wishlist wishlist = Wishlist.builder().id(1L).userId(USER_ID).build();
        wishlist.addItem(WishlistItem.builder().productId(PRODUCT_ID).build());

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
        Wishlist wishlist = Wishlist.builder().id(1L).userId(USER_ID).build();
        when(wishlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wishlist));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> wishlistService.removeFromWishlist(USER_ID, PRODUCT_ID));

        assertEquals(404, ex.getStatusCode());
    }
}
