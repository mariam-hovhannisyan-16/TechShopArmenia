package am.techshop.product.controller;

import am.techshop.common.dto.request.DiscountUpdateRequest;
import am.techshop.common.dto.request.PriceUpdateRequest;
import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.request.StockAdjustmentRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

@RestController
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    @PostMapping("/api/products")
    public ResponseEntity<ApiResponse<ProductResponse>> addProduct(
            @RequestBody @Valid ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product created", productService.addProduct(request)));
    }

    @GetMapping("/api/products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProducts(category, search, page, size)));
    }

    @GetMapping("/api/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductById(id)));
    }

    @DeleteMapping("/api/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product deleted", null));
    }

    @PatchMapping("/api/products/{id}/stock")
    public ResponseEntity<ApiResponse<ProductResponse>> adjustStock(
            @PathVariable Long id,
            @RequestBody @Valid StockAdjustmentRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey) {
        if (!internalApiKey.equals(apiKey)) {
            throw new TechShopException("Forbidden", 403);
        }
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted", productService.adjustStock(id, request.quantityDelta())));
    }

    @PutMapping("/api/products/{id}/price")
    public ResponseEntity<ApiResponse<ProductResponse>> updatePrice(
            @PathVariable Long id,
            @RequestBody @Valid PriceUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Price updated", productService.updatePrice(id, request.price())));
    }

    @PutMapping("/api/products/{id}/discount")
    public ResponseEntity<ApiResponse<ProductResponse>> updateDiscount(
            @PathVariable Long id,
            @RequestBody @Valid DiscountUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Discount updated", productService.updateDiscount(id, request.discountPercentage())));
    }
}
