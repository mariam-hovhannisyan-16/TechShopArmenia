package am.techshop.order.controller;

import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.OrderStatusUpdateRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatisticsResponse;
import am.techshop.common.dto.response.OrderStatusHistoryResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestBody @Valid CheckoutRequest request, Authentication authentication) {
        OrderResponse response = orderService.checkout(currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getUserOrders(currentUserId(authentication))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable @Positive Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(currentUserId(authentication), id)));
    }

    @GetMapping("/{id}/tracking")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderTracking(
            @PathVariable @Positive Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderTracking(currentUserId(authentication), id)));
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<OrderResponse>> payOrder(
            @PathVariable @Positive Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Order paid", orderService.payOrder(currentUserId(authentication), id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable @Positive Long id, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", orderService.cancelOrder(currentUserId(authentication), id)));
    }

    @GetMapping("/admin")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(status, page, size)));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderByIdAdmin(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderByIdAdmin(id)));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable @Positive Long id, @RequestBody @Valid OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Order status updated", orderService.updateOrderStatus(id, request)));
    }

    @GetMapping("/admin/statistics")
    public ResponseEntity<ApiResponse<OrderStatisticsResponse>> getStatistics() {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getStatistics()));
    }

    private Long currentUserId(Authentication authentication) {
        return (Long) authentication.getPrincipal();
    }
}
