package am.techshop.order.controller;

import am.techshop.common.dto.response.AdminStatsResponse;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.order.stats.OrderStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Stats", description = "Aggregated order and revenue statistics for admins")
@SecurityRequirement(name = "bearerAuth")
public class AdminStatsController {

    private final OrderStatsService orderStatsService;

    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide admin dashboard statistics")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(orderStatsService.getAdminStats()));
    }
}
