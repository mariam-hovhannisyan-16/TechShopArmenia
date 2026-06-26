package am.techshop.order.service;

import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.event.OrderCreatedEvent;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.client.CartClient;
import am.techshop.order.client.UserClient;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import am.techshop.order.kafka.OrderEventProducer;
import am.techshop.order.mapper.OrderMapper;
import am.techshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartClient cartClient;
    private final UserClient userClient;
    private final OrderEventProducer orderEventProducer;
    private final OrderMapper orderMapper;

    public OrderResponse createOrderFromCart(Long userId) {
        CartResponse cart = cartClient.getCart(userId);

        if (cart.items().isEmpty()) {
            throw new TechShopException("Cart is empty", 400);
        }

        UserResponse user = userClient.getUser(userId);

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalPrice(cart.totalPrice());
        order.setStatus(OrderStatus.NEW);

        cart.items().forEach(cartItem -> {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(cartItem.productId());
            item.setProductName(cartItem.productName());
            item.setProductPrice(cartItem.productPrice());
            item.setQuantity(cartItem.quantity());
            order.getItems().add(item);
        });

        Order saved = orderRepository.save(order);
        cartClient.clearCart(userId);

        orderEventProducer.sendOrderCreatedEvent(
                new OrderCreatedEvent(
                        saved.getId(),
                        saved.getUserId(),
                        user.email(),
                        user.name(),
                        saved.getTotalPrice()
                )
        );

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(orderMapper::toResponse)
                .orElseThrow(() -> new TechShopException("Order not found", 404));
    }

    public OrderResponse markAsPaid(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new TechShopException("Order not found", 404));
        order.setStatus(OrderStatus.PAID);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new TechShopException("Order not found", 404));
        if (order.getStatus() == OrderStatus.PAID) {
            throw new TechShopException("Paid order cannot be cancelled", 400);
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderMapper.toResponse(orderRepository.save(order));
    }
}