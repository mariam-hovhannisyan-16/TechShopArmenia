package am.techshop.cart.service;

import am.techshop.cart.entity.Cart;
import am.techshop.cart.entity.CartItem;
import am.techshop.cart.mapper.CartMapper;
import am.techshop.cart.repository.CartRepository;
import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.cart.client.ProductClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final ProductClient productClient;

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new TechShopException("Cart not found", 404));
        return cartMapper.toResponse(cart);
    }

    public CartResponse addItem(Long userId, AddItemRequest request) {
        ProductResponse product = productClient.getProduct(request.productId());

        if (product.quantity() < request.quantity()) {
            throw new TechShopException("Not enough stock", 400);
        }

        Cart cart = getOrCreate(userId);

        CartItem item = new CartItem();
        item.setProductId(product.id());
        item.setProductName(product.name());
        item.setProductPrice(product.price());
        item.setQuantity(request.quantity());

        cart.addOrUpdateItem(item);
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new TechShopException("Cart not found", 404));
        cart.removeItem(productId);
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    public void clearCart(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private Cart getOrCreate(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    return cartRepository.save(cart);
                });
    }
}