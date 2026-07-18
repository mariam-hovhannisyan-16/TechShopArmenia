package am.techshop.order.client;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "techshop-user", url = "${services.user.url:http://localhost:8081}")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    ApiResponse<UserResponse> getUser(@PathVariable("id") Long userId);
}