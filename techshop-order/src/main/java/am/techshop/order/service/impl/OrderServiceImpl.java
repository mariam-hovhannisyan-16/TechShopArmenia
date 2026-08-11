package am.techshop.order.service.impl;

import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.OrderStatusUpdateRequest;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatusHistoryResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.common.event.OrderStatusChangedEvent;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.client.CartClient;
import am.techshop.order.client.UserClient;
import am.techshop.order.entity.Address;
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
import am.techshop.order.stock.StockReservationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final UserClient userClient;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;
    private final PaymentProviderFactory paymentProviderFactory;
    private final StockReservationService stockReservationService;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        CartResponse cart = fetchCart(userId);

        if (cart.items().isEmpty()) {
            throw new TechShopException("Cart is empty", 400);
        }

        if (request.paymentMethod() != PaymentMethod.INSTALLMENT && request.installmentDetails() != null) {
            throw new TechShopException("Installment details are only allowed for INSTALLMENT payments", 400);
        }

        stockReservationService.reserve(cart.items());

        try {
            Address shippingAddress = orderMapper.toAddress(request.shippingAddress());
            Address billingAddress = orderMapper.toAddress(request.billingAddress());
            Order order = orderMapper.toEntity(request, userId, cart.totalPrice(), shippingAddress, billingAddress);

            cart.items().forEach(cartItem -> order.getItems().add(orderMapper.toOrderItem(cartItem, order)));

            order.transitionTo(OrderStatus.PENDING, "Order created from cart");

            PaymentProvider paymentProvider = paymentProviderFactory.resolve(request.paymentMethod());
            PaymentInitiationResult paymentResult = paymentProvider.createPayment(order);
            order.setPaymentMethod(request.paymentMethod());
            order.setPaymentReference(paymentResult.paymentReference());
            order.setPaymentStatus(PaymentStatus.PENDING);

            Order saved = orderRepository.save(order);

            try {
                cartClient.clearCart(userId, internalApiKey);
            } catch (FeignException ex) {
                log.warn("Failed to clear cart for user {} after checkout: {}", userId, ex.getMessage());
            }
            publishStatusChanged(saved, "Order created from cart");

            return orderMapper.toResponse(saved, paymentResult.redirectUrl());
        } catch (RuntimeException ex) {
            stockReservationService.restore(cart.items());
            throw ex;
        }
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
        applyTransition(order, OrderStatus.PAID, "Payment verified via %s".formatted(order.getPaymentMethod()));
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
    public PageResponse<OrderResponse> getAllOrders(OrderStatus status, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result;
        if (userId != null && status != null) {
            result = orderRepository.findByUserIdAndStatus(userId, status, pageable);
        } else if (userId != null) {
            result = orderRepository.findByUserId(userId, pageable);
        } else if (status != null) {
            result = orderRepository.findByStatus(status, pageable);
        } else {
            result = orderRepository.findAll(pageable);
        }

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

    private CartResponse fetchCart(Long userId) {
        try {
            var response = cartClient.getCart(userId, internalApiKey);
            if (response == null || response.data() == null) {
                throw new TechShopException("Cart is empty", 400);
            }
            return response.data();
        } catch (FeignException.NotFound ex) {
            throw new TechShopException("Cart is empty", 400);
        } catch (FeignException ex) {
            throw new TechShopException("Cart service unavailable", 503);
        }
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
        if (order.getStatus().isTerminal()) {
            throw new TechShopException(
                    "Order %s is already in a terminal state: %s".formatted(order.getId(), order.getStatus()), 409);
        }
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new TechShopException(
                    "Cannot transition order from %s to %s".formatted(order.getStatus(), newStatus), 409);
        }
        order.transitionTo(newStatus, note);
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REFUNDED) {
            stockReservationService.restore(order);
        }
        publishStatusChanged(order, note);
    }

    private void publishStatusChanged(Order order, String note) {
        try {
            UserResponse user = userClient.getUser(order.getUserId(), internalApiKey).data();
            List<String> productNames = order.getItems().stream().map(OrderItem::getProductName).toList();
            orderEventProducer.sendOrderStatusChangedEvent(new OrderStatusChangedEvent(
                    order.getId(), order.getUserId(), user.email(), user.name(),
                    order.getStatus(), note, order.getTotalPrice(),
                    order.getPaymentMethod(), orderMapper.toInstallmentPlanResponse(order.getInstallmentPlan()),
                    order.getLanguage(), productNames));
        } catch (Exception ex) {
            log.warn("Failed to publish status-changed notification for order {}: {}", order.getId(), ex.getMessage());
        }
    }

    public void anonymizeOrdersForUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        orders.forEach(order -> {
            redactAddress(order.getShippingAddress());
            redactAddress(order.getBillingAddress());
        });
        orderRepository.saveAll(orders);
    }

    private void redactAddress(Address address) {
        if (address == null) {
            return;
        }
        address.setFullName("Deleted User");
        address.setPhone("[redacted]");
        address.setLine1("[redacted]");
        address.setLine2(null);
    }
}
