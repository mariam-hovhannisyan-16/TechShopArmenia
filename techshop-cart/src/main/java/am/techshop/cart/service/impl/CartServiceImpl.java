package am.techshop.cart.service.impl;

import am.techshop.cart.entity.Cart;
import am.techshop.cart.entity.CartItem;
import am.techshop.cart.mapper.CartMapper;
import am.techshop.cart.repository.CartRepository;
import am.techshop.cart.service.CartService;
import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.cart.client.ProductClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

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
        ProductResponse product = fetchProduct(request.productId());

        Cart cart = getOrCreate(userId);

        CartItem item = CartItem.builder()
                .productId(product.id())
                .productName(product.name())
                .productPrice(product.price())
                .quantity(request.quantity())
                .build();

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
        cartRepository.findByUserId(userId).ifPresent(cart ->
                cart.getItems().clear()
        );
    }

    private ProductResponse fetchProduct(Long productId) {
        try {
            var response = productClient.getProduct(productId);
            if (response == null || response.data() == null) {
                throw new TechShopException("Product not found", 404);
            }
            return response.data();
        } catch (FeignException.NotFound ex) {
            throw new TechShopException("Product not found", 404);
        } catch (FeignException ex) {
            throw new TechShopException("Product service unavailable", 503);
        }
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