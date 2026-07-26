package am.techshop.order.service;

import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.OrderStatusUpdateRequest;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatusHistoryResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponse checkout(Long userId, CheckoutRequest request);

    List<OrderResponse> getUserOrders(Long userId);

    OrderResponse getOrderById(Long userId, Long id);

    List<OrderStatusHistoryResponse> getOrderTracking(Long userId, Long id);

    OrderResponse payOrder(Long userId, Long id);

    OrderResponse cancelOrder(Long userId, Long id);

    PageResponse<OrderResponse> getAllOrders(OrderStatus status, Long userId, int page, int size);

    OrderResponse getOrderByIdAdmin(Long id);

    OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request);
}
