package am.techshop.cart.controller;

import am.techshop.cart.security.CartAccessGuard;
import am.techshop.cart.service.CartService;
import am.techshop.common.dto.request.AddItemRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Manage a user's shopping cart")
public class CartController {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Api-Key";

    private final CartService cartService;
    private final CartAccessGuard cartAccessGuard;

    @GetMapping("/api/cart/{userId}")
    @Operation(
            summary = "Get a user's cart",
            description = "Accessible to the owning authenticated user, or via a valid internal service API key."
    )
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "ID of the cart owner", required = true)
            @PathVariable Long userId,
            Authentication authentication,
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String apiKey) {
        cartAccessGuard.verifyAccess(userId, authentication, apiKey);
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @PostMapping("/api/cart/{userId}/items")
    @Operation(
            summary = "Add an item to a user's cart",
            description = "Accessible to the owning authenticated user, or via a valid internal service API key."
    )
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Parameter(description = "ID of the cart owner", required = true)
            @PathVariable Long userId,
            @RequestBody @Valid AddItemRequest request,
            Authentication authentication,
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String apiKey) {
        cartAccessGuard.verifyAccess(userId, authentication, apiKey);
        return ResponseEntity.ok(ApiResponse.ok("Item added", cartService.addItem(userId, request)));
    }

    @DeleteMapping("/api/cart/{userId}/items/{productId}")
    @Operation(
            summary = "Remove an item from a user's cart",
            description = "Accessible to the owning authenticated user, or via a valid internal service API key."
    )
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @Parameter(description = "ID of the cart owner", required = true)
            @PathVariable Long userId,
            @Parameter(description = "ID of the product to remove", required = true)
            @PathVariable Long productId,
            Authentication authentication,
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String apiKey) {
        cartAccessGuard.verifyAccess(userId, authentication, apiKey);
        return ResponseEntity.ok(ApiResponse.ok("Item removed", cartService.removeItem(userId, productId)));
    }

    @DeleteMapping("/api/cart/{userId}/clear")
    @Operation(
            summary = "Remove all items from a user's cart",
            description = "Accessible to the owning authenticated user, or via a valid internal service API key."
    )
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @Parameter(description = "ID of the cart owner", required = true)
            @PathVariable Long userId,
            Authentication authentication,
            @RequestHeader(value = INTERNAL_KEY_HEADER, required = false) String apiKey) {
        cartAccessGuard.verifyAccess(userId, authentication, apiKey);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}