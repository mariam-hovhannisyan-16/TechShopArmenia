package am.techshop.cart.service;

import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, AddItemRequest request);
    CartResponse removeItem(Long userId, Long productId);
    void clearCart(Long userId);
}