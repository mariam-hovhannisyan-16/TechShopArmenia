package am.techshop.order.controller;

import am.techshop.common.dto.response.AdminStatsResponse;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.order.stats.OrderStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminStatsController {

    private final OrderStatsService orderStatsService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(orderStatsService.getAdminStats()));
    }
}
