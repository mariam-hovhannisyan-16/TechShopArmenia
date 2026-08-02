package am.techshop.cart.service;

import am.techshop.cart.client.ProductClient;
import am.techshop.cart.entity.Cart;
import am.techshop.cart.entity.CartItem;
import am.techshop.cart.mapper.CartMapper;
import am.techshop.cart.repository.CartRepository;
import am.techshop.cart.service.impl.CartServiceImpl;
import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.exception.TechShopException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartMapper cartMapper;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    void getCart_WhenCartExists_ReturnsCartResponse() {
        Long userId = 1L;
        Cart cart = new Cart();
        CartResponse cartResponse = new CartResponse(1L, userId, new ArrayList<>(), BigDecimal.ZERO);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);

        CartResponse result = cartService.getCart(userId);

        assertNotNull(result);
        assertEquals(userId, result.userId());
    }

    @Test
    void getCart_WhenCartNotFound_ThrowsException() {
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> cartService.getCart(userId));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void addItem_WhenProductExists_AddsItemToCart() {
        Long userId = 1L;
        AddItemRequest request = new AddItemRequest(1L, 2);
        ProductResponse product = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
        CartItem newItem = new CartItem();
        newItem.setProductId(1L);
        newItem.setProductName("Phone");
        newItem.setProductPrice(BigDecimal.valueOf(100));
        newItem.setQuantity(2);
        CartResponse cartResponse = new CartResponse(1L, userId, new ArrayList<>(), BigDecimal.valueOf(200));

        when(productClient.getProduct(request.productId())).thenReturn(new ApiResponse<>(true, "Success", product));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartMapper.toEntity(product, 2)).thenReturn(newItem);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);

        CartResponse result = cartService.addItem(userId, request);

        assertNotNull(result);
        verify(productClient).getProduct(request.productId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItem_WhenQuantityExceedsStock_ThrowsException() {
        Long userId = 1L;
        AddItemRequest request = new AddItemRequest(1L, 5);
        ProductResponse product = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 3, "Phones", null, false, null);
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());

        when(productClient.getProduct(request.productId())).thenReturn(new ApiResponse<>(true, "Success", product));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> cartService.addItem(userId, request));

        assertEquals(409, ex.getStatusCode());
        verify(cartRepository, times(0)).save(any(Cart.class));
    }

    @Test
    void addItem_WhenExistingPlusRequestedExceedsStock_ThrowsException() {
        Long userId = 1L;
        AddItemRequest request = new AddItemRequest(1L, 2);
        ProductResponse product = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 3, "Phones", null, false, null);
        CartItem existingItem = new CartItem();
        existingItem.setProductId(1L);
        existingItem.setQuantity(2);
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>(java.util.List.of(existingItem)));

        when(productClient.getProduct(request.productId())).thenReturn(new ApiResponse<>(true, "Success", product));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> cartService.addItem(userId, request));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void addItem_WhenNoCartExistsAndConcurrentInsertRaces_ReusesWinningRow() {
        Long userId = 1L;
        AddItemRequest request = new AddItemRequest(1L, 1);
        ProductResponse product = new ProductResponse(1L, "Phone", "Desc", BigDecimal.valueOf(100), 10, "Phones", null, false, null);
        Cart winningCart = new Cart();
        winningCart.setUserId(userId);
        winningCart.setItems(new ArrayList<>());
        CartItem newItem = new CartItem();
        newItem.setProductId(1L);
        newItem.setQuantity(1);
        CartResponse cartResponse = new CartResponse(1L, userId, new ArrayList<>(), BigDecimal.valueOf(100));

        when(productClient.getProduct(request.productId())).thenReturn(new ApiResponse<>(true, "Success", product));
        when(cartRepository.findByUserId(userId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winningCart));
        when(cartRepository.save(any(Cart.class)))
                .thenThrow(new DataIntegrityViolationException("uk_cart_user"))
                .thenReturn(winningCart);
        when(cartMapper.toEntity(product, 1)).thenReturn(newItem);
        when(cartMapper.toResponse(winningCart)).thenReturn(cartResponse);

        CartResponse result = cartService.addItem(userId, request);

        assertNotNull(result);
        verify(cartRepository, times(2)).findByUserId(userId);
    }

    @Test
    void addItem_WhenProductNotFound_ThrowsException() {
        Long userId = 1L;
        AddItemRequest request = new AddItemRequest(1L, 2);

        when(productClient.getProduct(request.productId()))
                .thenReturn(new ApiResponse<>(false, "Not found", null));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> cartService.addItem(userId, request));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void removeItem_WhenCartExists_RemovesItem() {
        Long userId = 1L;
        Long productId = 1L;
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());
        CartResponse cartResponse = new CartResponse(1L, userId, new ArrayList<>(), BigDecimal.ZERO);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        when(cartMapper.toResponse(cart)).thenReturn(cartResponse);

        CartResponse result = cartService.removeItem(userId, productId);

        assertNotNull(result);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void removeItem_WhenCartNotFound_ThrowsException() {
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> cartService.removeItem(userId, 1L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void clearCart_WhenCartExists_ClearsItems() {
        Long userId = 1L;
        Cart cart = new Cart();
        cart.setItems(new ArrayList<>());

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        cartService.clearCart(userId);

        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void deleteCartForUser_WhenCartExists_DeletesIt() {
        Long userId = 1L;
        Cart cart = new Cart();
        cart.setUserId(userId);

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        cartService.deleteCartForUser(userId);

        verify(cartRepository).delete(cart);
    }

    @Test
    void deleteCartForUser_WhenNoCartExists_DoesNothing() {
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        cartService.deleteCartForUser(userId);

        verify(cartRepository, never()).delete(any());
    }
}