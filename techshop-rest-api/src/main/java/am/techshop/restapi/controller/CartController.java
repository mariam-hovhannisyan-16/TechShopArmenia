package am.techshop.restapi.controller;

import am.techshop.cart.service.CartService;
import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CartResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/api/cart/{userId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @PostMapping("/api/cart/{userId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @PathVariable Long userId,
            @RequestBody @Valid AddItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Item added", cartService.addItem(userId, request)));
    }

    @DeleteMapping("/api/cart/{userId}/items/{productId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @PathVariable Long userId,
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok("Item removed", cartService.removeItem(userId, productId)));
    }

    @DeleteMapping("/api/cart/{userId}/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}