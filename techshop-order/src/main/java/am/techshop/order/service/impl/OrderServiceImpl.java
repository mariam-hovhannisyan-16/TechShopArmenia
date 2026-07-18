package am.techshop.order.service.impl;

import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.OrderStatusUpdateRequest;
import am.techshop.common.dto.request.StockAdjustmentRequest;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatisticsResponse;
import am.techshop.common.dto.response.OrderStatusHistoryResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.common.event.OrderCreatedEvent;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.client.CartClient;
import am.techshop.order.client.ProductClient;
import am.techshop.order.client.UserClient;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import am.techshop.order.kafka.OrderEventProducer;
import am.techshop.order.mapper.OrderMapper;
import am.techshop.order.payment.PaymentInitiationResult;
import am.techshop.order.payment.PaymentProvider;
import am.techshop.order.payment.PaymentProviderFactory;
import am.techshop.order.payment.PaymentVerificationResult;
import am.techshop.order.repository.OrderRepository;
import am.techshop.order.service.OrderService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final List<OrderStatus> REVENUE_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final UserClient userClient;
    private final ProductClient productClient;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;
    private final PaymentProviderFactory paymentProviderFactory;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        CartResponse cart = cartClient.getCart(userId).data();

        if (cart.items().isEmpty()) {
            throw new TechShopException("Cart is empty", 400);
        }

        reserveStock(cart.items());

        UserResponse user = userClient.getUser(userId).data();

        Order order = Order.builder()
                .userId(userId)
                .totalPrice(cart.totalPrice())
                .shippingAddress(orderMapper.toAddress(request.shippingAddress()))
                .billingAddress(orderMapper.toAddress(request.billingAddress()))
                .notes(request.notes())
                .build();

        cart.items().forEach(cartItem -> order.getItems().add(
                OrderItem.builder()
                        .order(order)
                        .productId(cartItem.productId())
                        .productName(cartItem.productName())
                        .productPrice(cartItem.productPrice())
                        .quantity(cartItem.quantity())
                        .build()
        ));

        order.transitionTo(OrderStatus.PENDING, "Order created from cart");

        PaymentProvider paymentProvider = paymentProviderFactory.resolve(request.paymentMethod());
        PaymentInitiationResult paymentResult = paymentProvider.createPayment(order);
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentReference(paymentResult.paymentReference());
        order.setPaymentStatus(PaymentStatus.PENDING);

        Order saved = orderRepository.save(order);
        cartClient.clearCart(userId);

        orderEventProducer.sendOrderCreatedEvent(
                new OrderCreatedEvent(saved.getId(), saved.getUserId(), user.email(), user.name(), saved.getTotalPrice())
        );

        return orderMapper.toResponse(saved, paymentResult.redirectUrl());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long id) {
        return orderMapper.toResponse(getOwnedOrder(userId, id));
    }

    @Transactional(readOnly = true)
    public List<OrderStatusHistoryResponse> getOrderTracking(Long userId, Long id) {
        return getOwnedOrder(userId, id).getStatusHistory().stream()
                .map(orderMapper::toHistoryResponse)
                .toList();
    }

    public OrderResponse payOrder(Long userId, Long id) {
        Order order = getOwnedOrder(userId, id);

        PaymentProvider paymentProvider = paymentProviderFactory.resolve(order.getPaymentMethod());
        PaymentVerificationResult verification = paymentProvider.verifyPayment(order.getPaymentReference());

        if (verification.status() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new TechShopException("Payment not completed", 402);
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        applyTransition(order, OrderStatus.PAID, "Payment verified via " + order.getPaymentMethod());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse cancelOrder(Long userId, Long id) {
        Order order = getOwnedOrder(userId, id);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new TechShopException("Order can no longer be cancelled", 409);
        }
        applyTransition(order, OrderStatus.CANCELLED, "Cancelled by customer");
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAllOrders(OrderStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = status != null
                ? orderRepository.findByStatus(status, pageable)
                : orderRepository.findAll(pageable);

        return new PageResponse<>(
                result.getContent().stream().map(orderMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new TechShopException("Order not found", 404));
    }

    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new TechShopException("Order not found", 404));
        applyTransition(order, request.status(), request.note());
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderStatisticsResponse getStatistics() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalPriceByStatusIn(REVENUE_STATUSES);
        long revenueOrderCount = orderRepository.countByStatusIn(REVENUE_STATUSES);

        BigDecimal averageOrderValue = revenueOrderCount == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(revenueOrderCount), 2, RoundingMode.HALF_UP);

        Map<OrderStatus, Long> ordersByStatus = orderRepository.countGroupedByStatus().stream()
                .collect(Collectors.toMap(row -> (OrderStatus) row[0], row -> (Long) row[1]));

        return new OrderStatisticsResponse(totalOrders, totalRevenue, averageOrderValue, ordersByStatus);
    }

    private Order getOwnedOrder(Long userId, Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new TechShopException("Order not found", 404));
        if (!order.getUserId().equals(userId)) {
            throw new TechShopException("Order not found", 404);
        }
        return order;
    }

    private void applyTransition(Order order, OrderStatus newStatus, String note) {
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new TechShopException(
                    "Cannot transition order from " + order.getStatus() + " to " + newStatus, 409);
        }
        order.transitionTo(newStatus, note);
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REFUNDED) {
            restoreOrderStock(order);
        }
    }

    private void reserveStock(List<CartItemResponse> items) {
        List<CartItemResponse> reserved = new ArrayList<>();
        try {
            for (CartItemResponse item : items) {
                adjustStock(item.productId(), -item.quantity());
                reserved.add(item);
            }
        } catch (TechShopException ex) {
            reserved.forEach(item -> adjustStockQuietly(item.productId(), item.quantity()));
            throw ex;
        }
    }

    private void restoreOrderStock(Order order) {
        order.getItems().forEach(item -> adjustStockQuietly(item.getProductId(), item.getQuantity()));
    }

    private void adjustStock(Long productId, int delta) {
        try {
            productClient.adjustStock(productId, new StockAdjustmentRequest(delta), internalApiKey);
        } catch (FeignException.Conflict ex) {
            throw new TechShopException("Insufficient stock for product " + productId, 409);
        } catch (FeignException ex) {
            throw new TechShopException("Product service unavailable", 503);
        }
    }

    private void adjustStockQuietly(Long productId, int delta) {
        try {
            productClient.adjustStock(productId, new StockAdjustmentRequest(delta), internalApiKey);
        } catch (FeignException ignored) {
            // Best-effort restock/compensation: a failure here must not block the status
            // change or mask the original error that triggered it.
        }
    }
}
