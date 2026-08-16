package am.techshop.notification.client;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.UserLanguageResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "techshop-user", url = "${services.user.url:http://localhost:8081}")
public interface UserClient {

    @GetMapping("/api/users/internal/languages")
    ApiResponse<List<UserLanguageResponse>> getUserLanguages(@RequestParam("ids") List<Long> ids, @RequestHeader("X-Internal-Api-Key") String apiKey);
}
