package am.techshop.order.client;

import am.techshop.common.dto.request.StockAdjustmentRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "techshop-product", url = "${services.product.url:http://localhost:8084}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable("id") Long id);

    @PatchMapping("/api/products/{id}/stock")
    ApiResponse<ProductResponse> adjustStock(
            @PathVariable("id") Long id,
            @RequestBody StockAdjustmentRequest request,
            @RequestHeader("X-Internal-Api-Key") String apiKey);
}
