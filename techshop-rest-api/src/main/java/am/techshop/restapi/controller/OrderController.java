package am.techshop.restapi.controller;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/orders/{userId}")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@PathVariable Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order created", orderService.createOrderFromCart(userId)));
    }

    @GetMapping("/api/orders/user/{userId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getUserOrders(userId)));
    }

    @GetMapping("/api/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(id)));
    }

    @PatchMapping("/api/orders/{id}/pay")
    public ResponseEntity<ApiResponse<OrderResponse>> payOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Order paid", orderService.markAsPaid(id)));
    }

    @PatchMapping("/api/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled", orderService.cancelOrder(id)));
    }
}
