package am.techshop.product.client;

import am.techshop.common.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "techshop-wishlist", url = "${services.wishlist.url:http://localhost:8086}")
public interface WishlistClient {

    @GetMapping("/wishlist/products/{productId}/subscribers")
    ApiResponse<List<Long>> getSubscribers(@PathVariable Long productId, @RequestHeader("X-Internal-Api-Key") String apiKey);
}
