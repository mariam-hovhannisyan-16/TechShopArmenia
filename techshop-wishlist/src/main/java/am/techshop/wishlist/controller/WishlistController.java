package am.techshop.wishlist.controller;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.WishlistResponse;
import am.techshop.wishlist.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wishlist")
@RequiredArgsConstructor
@Validated
@Tag(name = "Wishlist", description = "Manage the current authenticated user's product wishlist")
@SecurityRequirement(name = "bearerAuth")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/products/{productId}")
    @Operation(
            summary = "Add a product to the current user's wishlist",
            description = "Returns 409 if the product is already in the wishlist and 404 if the product does not exist."
    )
    public ResponseEntity<ApiResponse<WishlistResponse>> addProduct(
            @Parameter(description = "ID of the product to add", required = true)
            @PathVariable @Positive Long productId,
            Authentication authentication) {
        WishlistResponse response = wishlistService.addToWishlist(currentUserId(authentication), productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product added to wishlist", response));
    }

    @GetMapping
    @Operation(summary = "Get the current user's wishlist")
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlist(currentUserId(authentication))));
    }

    @GetMapping("/count")
    @Operation(summary = "Get the number of items in the current user's wishlist")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlistCount(currentUserId(authentication))));
    }

    @DeleteMapping("/products/{productId}")
    @Operation(
            summary = "Remove a product from the current user's wishlist",
            description = "Returns 404 if the wishlist or the product within it does not exist."
    )
    public ResponseEntity<ApiResponse<WishlistResponse>> removeProduct(
            @Parameter(description = "ID of the product to remove", required = true)
            @PathVariable @Positive Long productId,
            Authentication authentication) {
        WishlistResponse response = wishlistService.removeFromWishlist(currentUserId(authentication), productId);
        return ResponseEntity.ok(ApiResponse.ok("Product removed from wishlist", response));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
