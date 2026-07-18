package am.techshop.wishlist.client;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "techshop-product", url = "${services.product.url:http://localhost:8084}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProduct(@PathVariable Long id);
}
