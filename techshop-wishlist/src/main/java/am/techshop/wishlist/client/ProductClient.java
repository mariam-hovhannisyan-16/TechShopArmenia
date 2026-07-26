package am.techshop.wishlist.client;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "techshop-product", url = "${services.product.url:http://localhost:8084}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable Long id);

    @GetMapping("/api/products/batch")
    ApiResponse<List<ProductResponse>> getProducts(@RequestParam List<Long> ids);
}
