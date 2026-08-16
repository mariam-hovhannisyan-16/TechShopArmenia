package am.techshop.product.controller;

import am.techshop.common.dto.request.DiscountUpdateRequest;
import am.techshop.common.dto.request.PriceUpdateRequest;
import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.request.RatingUpdateRequest;
import am.techshop.common.dto.request.StockAdjustmentRequest;
import am.techshop.common.dto.request.StockUpdateRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.security.InternalApiKeyGuard;
import am.techshop.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Browse and manage the product catalog")
public class ProductController {

    private final ProductService productService;
    private final InternalApiKeyGuard internalApiKeyGuard;

    @PostMapping("/api/products")
    @Operation(summary = "Create a new product")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ProductResponse>> addProduct(
            @RequestBody @Valid ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", productService.addProduct(request)));
    }

    @GetMapping("/api/products")
    @Operation(summary = "List products, optionally filtered by category or search term")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProducts(category, search, page, size)));
    }

    @GetMapping("/api/products/{id}")
    @Operation(summary = "Get a product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "ID of the product to fetch", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductById(id)));
    }

    @GetMapping("/api/products/batch")
    @Operation(summary = "Get multiple products by their IDs")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProductsByIds(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductsByIds(ids)));
    }

    @DeleteMapping("/api/products/{id}")
    @Operation(summary = "Delete a product")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "ID of the product to delete", required = true)
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted", null));
    }

    @PatchMapping("/api/products/{id}/stock")
    @Operation(
            summary = "Adjust a product's stock by a relative delta",
            description = "Internal service-to-service endpoint, guarded by an internal API key."
    )
    public ResponseEntity<ApiResponse<ProductResponse>> adjustStock(
            @Parameter(description = "ID of the product to adjust", required = true)
            @PathVariable Long id,
            @RequestBody @Valid StockAdjustmentRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        internalApiKeyGuard.verify(apiKey);
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted", productService.adjustStock(id, request.quantityDelta())));
    }

    @PutMapping("/api/products/{id}/stock")
    @Operation(summary = "Set a product's absolute stock quantity")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @Parameter(description = "ID of the product to update", required = true)
            @PathVariable Long id,
            @RequestBody @Valid StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Stock updated", productService.updateStock(id, request.quantity())));
    }

    @PutMapping("/api/products/{id}/price")
    @Operation(summary = "Update a product's price")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ProductResponse>> updatePrice(
            @Parameter(description = "ID of the product to update", required = true)
            @PathVariable Long id,
            @RequestBody @Valid PriceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Price updated", productService.updatePrice(id, request.price())));
    }

    @PutMapping("/api/products/{id}/discount")
    @Operation(summary = "Update a product's discount percentage")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ProductResponse>> updateDiscount(
            @Parameter(description = "ID of the product to update", required = true)
            @PathVariable Long id,
            @RequestBody @Valid DiscountUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Discount updated", productService.updateDiscount(id, request.discountPercentage())));
    }

    @PatchMapping("/api/products/{id}/rating")
    @Operation(
            summary = "Update a product's aggregate rating and review count",
            description = "Internal service-to-service endpoint, guarded by an internal API key."
    )
    public ResponseEntity<ApiResponse<ProductResponse>> updateRating(
            @Parameter(description = "ID of the product to update", required = true)
            @PathVariable Long id,
            @RequestBody @Valid RatingUpdateRequest request,
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        internalApiKeyGuard.verify(apiKey);
        return ResponseEntity.ok(ApiResponse.ok(
                "Rating updated", productService.updateRating(id, request.rating(), request.reviewCount())));
    }
}
