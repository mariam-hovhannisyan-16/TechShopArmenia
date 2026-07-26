package am.techshop.order.client;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CartResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "techshop-cart", url = "${services.cart.url:http://localhost:8082}")
public interface CartClient {

    @GetMapping("/api/cart/{userId}")
    ApiResponse<CartResponse> getCart(@PathVariable Long userId, @RequestHeader("X-Internal-Api-Key") String apiKey);

    @DeleteMapping("/api/cart/{userId}/clear")
    void clearCart(@PathVariable Long userId, @RequestHeader("X-Internal-Api-Key") String apiKey);
}