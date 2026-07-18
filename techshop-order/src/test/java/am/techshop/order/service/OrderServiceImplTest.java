package am.techshop.order.service;

import am.techshop.common.dto.request.AddressRequest;
import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.OrderStatusUpdateRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatisticsResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.common.enums.UserRole;
import am.techshop.common.event.OrderCreatedEvent;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.client.CartClient;
import am.techshop.order.client.ProductClient;
import am.techshop.order.client.UserClient;
import am.techshop.order.entity.Address;
import am.techshop.order.entity.Order;
import am.techshop.order.kafka.OrderEventProducer;
import am.techshop.order.mapper.OrderMapper;
import am.techshop.order.payment.PaymentInitiationResult;
import am.techshop.order.payment.PaymentProvider;
import am.techshop.order.payment.PaymentProviderFactory;
import am.techshop.order.payment.PaymentVerificationResult;
import am.techshop.order.repository.OrderRepository;
import am.techshop.order.service.impl.OrderServiceImpl;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartClient cartClient;

    @Mock
    private UserClient userClient;

    @Mock
    private ProductClient productClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentProviderFactory paymentProviderFactory;

    @Mock
    private PaymentProvider paymentProvider;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static final Long USER_ID = 1L;

    private static AddressRequest sampleAddress() {
        return new AddressRequest("Mariam A", "+374000000", "1 Main St", null, "Yerevan", null, "0001", "Armenia");
    }

    private static FeignException.Conflict conflict() {
        Request request = Request.create(Request.HttpMethod.PATCH, "/api/products/1/stock",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(409).reason("Conflict").request(request).build();
        return (FeignException.Conflict) FeignException.errorStatus("ProductClient#adjustStock", response);
    }

    private static OrderResponse sampleResponse(OrderStatus status) {
        return new OrderResponse(1L, USER_ID, List.of(), BigDecimal.ZERO, status, null, null, null, List.of(),
                PaymentMethod.IDRAM, "IDRAM-ref", PaymentStatus.PENDING, null, null, null);
    }

    @Test
    void checkout_WhenCartHasItems_ReservesStockAndCreatesOrder() {
        CartItemResponse cartItem = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(cartItem), BigDecimal.valueOf(200));
        UserResponse user = new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.USER, LocalDateTime.now(), true);
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), "Leave at door", PaymentMethod.IDRAM);

        when(cartClient.getCart(USER_ID)).thenReturn(new ApiResponse<>(true, "Success", cart));
        when(productClient.adjustStock(eq(1L), any(), any())).thenReturn(new ApiResponse<>(true, "ok", null));
        when(userClient.getUser(USER_ID)).thenReturn(new ApiResponse<>(true, "Success", user));
        when(orderMapper.toAddress(any(AddressRequest.class))).thenReturn(Address.builder().build());
        when(paymentProviderFactory.resolve(PaymentMethod.IDRAM)).thenReturn(paymentProvider);
        when(paymentProvider.createPayment(any(Order.class)))
                .thenReturn(new PaymentInitiationResult("IDRAM-ref", "https://sandbox.idram.am/payment?ref=IDRAM-ref", PaymentStatus.PENDING));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderMapper.toResponse(any(Order.class), any()))
                .thenReturn(new OrderResponse(1L, USER_ID, List.of(), BigDecimal.valueOf(200), OrderStatus.PENDING,
                        null, null, "Leave at door", List.of(), PaymentMethod.IDRAM, "IDRAM-ref", PaymentStatus.PENDING,
                        "https://sandbox.idram.am/payment?ref=IDRAM-ref", LocalDateTime.now(), null));

        OrderResponse result = orderService.checkout(USER_ID, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentStatus.PENDING, result.paymentStatus());
        assertNotNull(result.paymentRedirectUrl());
        verify(productClient).adjustStock(eq(1L), any(), any());
        verify(cartClient).clearCart(USER_ID);
        verify(orderEventProducer).sendOrderCreatedEvent(any(OrderCreatedEvent.class));
    }

    @Test
    void checkout_WhenCartIsEmpty_ThrowsException() {
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(), BigDecimal.ZERO);
        when(cartClient.getCart(USER_ID)).thenReturn(new ApiResponse<>(true, "Success", cart));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.checkout(USER_ID, new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.IDRAM)));

        assertEquals(400, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_WhenStockInsufficientForSecondItem_CompensatesFirstItemAndThrows() {
        CartItemResponse item1 = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        CartItemResponse item2 = new CartItemResponse(2L, "Laptop", BigDecimal.valueOf(500), 1, BigDecimal.valueOf(500));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(item1, item2), BigDecimal.valueOf(600));

        when(cartClient.getCart(USER_ID)).thenReturn(new ApiResponse<>(true, "Success", cart));
        when(productClient.adjustStock(eq(1L), any(), any())).thenReturn(new ApiResponse<>(true, "ok", null));
        when(productClient.adjustStock(eq(2L), any(), any())).thenThrow(conflict());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.checkout(USER_ID, new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.IDRAM)));

        assertEquals(409, ex.getStatusCode());
        // one reservation call for item 1, one failing reservation call for item 2, one compensating restore for item 1
        verify(productClient, times(2)).adjustStock(eq(1L), any(), any());
        verify(productClient, times(1)).adjustStock(eq(2L), any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void checkout_WhenPaymentMethodUnsupported_ThrowsBadRequest() {
        CartItemResponse cartItem = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(cartItem), BigDecimal.valueOf(100));

        when(cartClient.getCart(USER_ID)).thenReturn(new ApiResponse<>(true, "Success", cart));
        when(productClient.adjustStock(eq(1L), any(), any())).thenReturn(new ApiResponse<>(true, "ok", null));
        when(userClient.getUser(USER_ID)).thenReturn(new ApiResponse<>(true, "Success",
                new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.USER, LocalDateTime.now(), true)));
        when(orderMapper.toAddress(any(AddressRequest.class))).thenReturn(Address.builder().build());
        when(paymentProviderFactory.resolve(PaymentMethod.TELCELL))
                .thenThrow(new TechShopException("Unsupported payment method: TELCELL", 400));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.checkout(USER_ID, new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.TELCELL)));

        assertEquals(400, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_WhenOwnedByUser_ReturnsOrder() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PENDING));

        OrderResponse result = orderService.getOrderById(USER_ID, 1L);

        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
    }

    @Test
    void getOrderById_WhenNotOwnedByUser_ThrowsNotFound() {
        Order order = Order.builder().id(1L).userId(2L).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.getOrderById(USER_ID, 1L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void payOrder_WhenPendingAndPaymentVerified_TransitionsToPaid() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.IDRAM).paymentReference("IDRAM-ref").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(paymentProviderFactory.resolve(PaymentMethod.IDRAM)).thenReturn(paymentProvider);
        when(paymentProvider.verifyPayment("IDRAM-ref")).thenReturn(new PaymentVerificationResult(PaymentStatus.PAID, "ok"));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PAID));

        OrderResponse result = orderService.payOrder(USER_ID, 1L);

        assertEquals(OrderStatus.PAID, result.status());
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(1, order.getStatusHistory().size());
    }

    @Test
    void payOrder_WhenVerificationFails_ThrowsPaymentRequired() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PENDING)
                .paymentMethod(PaymentMethod.IDRAM).paymentReference("IDRAM-ref").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        lenient().when(orderRepository.save(order)).thenReturn(order);
        when(paymentProviderFactory.resolve(PaymentMethod.IDRAM)).thenReturn(paymentProvider);
        when(paymentProvider.verifyPayment("IDRAM-ref")).thenReturn(new PaymentVerificationResult(PaymentStatus.FAILED, "declined"));

        TechShopException ex = assertThrows(TechShopException.class, () -> orderService.payOrder(USER_ID, 1L));

        assertEquals(402, ex.getStatusCode());
        assertEquals(PaymentStatus.FAILED, order.getPaymentStatus());
        assertEquals(OrderStatus.PENDING, order.getStatus());
    }

    @Test
    void payOrder_WhenAlreadyPaid_ThrowsInvalidTransition() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PAID)
                .paymentMethod(PaymentMethod.IDRAM).paymentReference("IDRAM-ref").build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentProviderFactory.resolve(PaymentMethod.IDRAM)).thenReturn(paymentProvider);
        when(paymentProvider.verifyPayment("IDRAM-ref")).thenReturn(new PaymentVerificationResult(PaymentStatus.PAID, "ok"));

        TechShopException ex = assertThrows(TechShopException.class, () -> orderService.payOrder(USER_ID, 1L));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void cancelOrder_WhenPending_CancelsAndRestoresStock() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PENDING).build();
        order.getItems().add(am.techshop.order.entity.OrderItem.builder().productId(1L).quantity(2).build());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(productClient.adjustStock(eq(1L), any(), any())).thenReturn(new ApiResponse<>(true, "ok", null));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.CANCELLED));

        OrderResponse result = orderService.cancelOrder(USER_ID, 1L);

        assertEquals(OrderStatus.CANCELLED, result.status());
        verify(productClient).adjustStock(eq(1L), any(), any());
    }

    @Test
    void cancelOrder_WhenProcessing_ThrowsException() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PROCESSING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        TechShopException ex = assertThrows(TechShopException.class, () -> orderService.cancelOrder(USER_ID, 1L));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void updateOrderStatus_ValidTransition_Succeeds() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PAID).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PROCESSING));

        OrderResponse result = orderService.updateOrderStatus(1L, new OrderStatusUpdateRequest(OrderStatus.PROCESSING, "Packing"));

        assertEquals(OrderStatus.PROCESSING, result.status());
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PENDING).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.updateOrderStatus(1L, new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null)));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void getAllOrders_ReturnsPagedResults() {
        Order order = Order.builder().id(1L).userId(USER_ID).status(OrderStatus.PENDING).build();
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PENDING));

        PageResponse<OrderResponse> result = orderService.getAllOrders(null, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
    }

    @Test
    void getStatistics_ReturnsAggregates() {
        when(orderRepository.count()).thenReturn(5L);
        when(orderRepository.sumTotalPriceByStatusIn(any())).thenReturn(BigDecimal.valueOf(1000));
        when(orderRepository.countByStatusIn(any())).thenReturn(4L);
        when(orderRepository.countGroupedByStatus()).thenReturn(
                List.of(new Object[]{OrderStatus.PENDING, 1L}, new Object[]{OrderStatus.PAID, 4L}));

        OrderStatisticsResponse result = orderService.getStatistics();

        assertEquals(5L, result.totalOrders());
        assertEquals(BigDecimal.valueOf(1000), result.totalRevenue());
        assertEquals(BigDecimal.valueOf(250.00).setScale(2), result.averageOrderValue());
    }
}
