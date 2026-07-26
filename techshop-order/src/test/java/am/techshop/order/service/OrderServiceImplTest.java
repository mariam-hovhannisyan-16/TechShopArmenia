package am.techshop.order.service;

import am.techshop.common.dto.request.AddressRequest;
import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.InstallmentDetailsRequest;
import am.techshop.common.dto.request.OrderStatusUpdateRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.dto.response.CartResponse;
import am.techshop.common.dto.response.InstallmentPlanResponse;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatusHistoryResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.UserResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.common.enums.UserRole;
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
import am.techshop.order.service.impl.OrderServiceImpl;
import am.techshop.order.stock.StockReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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
    private StockReservationService stockReservationService;

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

    private static OrderResponse sampleResponse(OrderStatus status) {
        return new OrderResponse(1L, USER_ID, List.of(), BigDecimal.ZERO, status, null, null, null, List.of(),
                PaymentMethod.IDRAM, "IDRAM-ref", PaymentStatus.PENDING, null, null, null, null);
    }

    @Test
    void checkout_WhenCartHasItems_ReservesStockAndCreatesOrder() {
        CartItemResponse cartItem = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(cartItem), BigDecimal.valueOf(200));
        UserResponse user = new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), "Leave at door", PaymentMethod.IDRAM, null);

        when(cartClient.getCart(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", cart));
        when(userClient.getUser(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", user));
        when(orderMapper.toAddress(any(AddressRequest.class))).thenReturn(new Address());
        when(orderMapper.toEntity(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Order order = new Order();
            order.setUserId(inv.getArgument(1));
            return order;
        });
        when(orderMapper.toOrderItem(any(), any())).thenReturn(new OrderItem());
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
                        "https://sandbox.idram.am/payment?ref=IDRAM-ref", null, LocalDateTime.now(), null));

        OrderResponse result = orderService.checkout(USER_ID, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentStatus.PENDING, result.paymentStatus());
        assertNotNull(result.paymentRedirectUrl());
        verify(stockReservationService).reserve(List.of(cartItem));
        verify(stockReservationService, never()).restore(anyList());
        verify(cartClient).clearCart(eq(USER_ID), any());
        verify(orderEventProducer).sendOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void checkout_WithRoketLinePaymentMethod_ReservesStockAndCreatesOrder() {
        CartItemResponse cartItem = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(cartItem), BigDecimal.valueOf(200));
        UserResponse user = new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), "Leave at door", PaymentMethod.ROKET_LINE, null);

        when(cartClient.getCart(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", cart));
        when(userClient.getUser(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", user));
        when(orderMapper.toAddress(any(AddressRequest.class))).thenReturn(new Address());
        when(orderMapper.toEntity(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Order order = new Order();
            order.setUserId(inv.getArgument(1));
            return order;
        });
        when(orderMapper.toOrderItem(any(), any())).thenReturn(new OrderItem());
        when(paymentProviderFactory.resolve(PaymentMethod.ROKET_LINE)).thenReturn(paymentProvider);
        when(paymentProvider.createPayment(any(Order.class)))
                .thenReturn(new PaymentInitiationResult("ROKET-LINE-ref", "https://sandbox.roketline.am/payment?ref=ROKET-LINE-ref", PaymentStatus.PENDING));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderMapper.toResponse(any(Order.class), any()))
                .thenReturn(new OrderResponse(1L, USER_ID, List.of(), BigDecimal.valueOf(200), OrderStatus.PENDING,
                        null, null, "Leave at door", List.of(), PaymentMethod.ROKET_LINE, "ROKET-LINE-ref", PaymentStatus.PENDING,
                        "https://sandbox.roketline.am/payment?ref=ROKET-LINE-ref", null, LocalDateTime.now(), null));

        OrderResponse result = orderService.checkout(USER_ID, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.status());
        assertEquals(PaymentStatus.PENDING, result.paymentStatus());
        assertNotNull(result.paymentRedirectUrl());
        verify(stockReservationService).reserve(List.of(cartItem));
        verify(cartClient).clearCart(eq(USER_ID), any());
        verify(orderEventProducer).sendOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void checkout_WithInstallmentPaymentMethod_ReservesStockAndCreatesOrderWithPlan() {
        CartItemResponse cartItem = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(cartItem), BigDecimal.valueOf(200));
        UserResponse user = new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        InstallmentDetailsRequest installmentDetails = new InstallmentDetailsRequest("Ameriabank", 12, BigDecimal.valueOf(20));
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), "Leave at door", PaymentMethod.INSTALLMENT, installmentDetails);
        InstallmentPlanResponse planResponse = new InstallmentPlanResponse("Ameriabank", BigDecimal.valueOf(0.12), 12, BigDecimal.valueOf(20), BigDecimal.valueOf(16.80));

        when(cartClient.getCart(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", cart));
        when(userClient.getUser(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", user));
        when(orderMapper.toAddress(any(AddressRequest.class))).thenReturn(new Address());
        when(orderMapper.toEntity(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Order order = new Order();
            order.setUserId(inv.getArgument(1));
            return order;
        });
        when(orderMapper.toOrderItem(any(), any())).thenReturn(new OrderItem());
        when(paymentProviderFactory.resolve(PaymentMethod.INSTALLMENT)).thenReturn(paymentProvider);
        when(paymentProvider.createPayment(any(Order.class)))
                .thenReturn(new PaymentInitiationResult("INSTALLMENT-ref", "https://sandbox.installments.am/payment?ref=INSTALLMENT-ref", PaymentStatus.PENDING));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(1L);
            return order;
        });
        when(orderMapper.toResponse(any(Order.class), any()))
                .thenReturn(new OrderResponse(1L, USER_ID, List.of(), BigDecimal.valueOf(200), OrderStatus.PENDING,
                        null, null, "Leave at door", List.of(), PaymentMethod.INSTALLMENT, "INSTALLMENT-ref", PaymentStatus.PENDING,
                        "https://sandbox.installments.am/payment?ref=INSTALLMENT-ref", planResponse, LocalDateTime.now(), null));

        OrderResponse result = orderService.checkout(USER_ID, request);

        assertNotNull(result);
        assertEquals(OrderStatus.PENDING, result.status());
        assertNotNull(result.installmentPlan());
        assertEquals("Ameriabank", result.installmentPlan().bankName());
        verify(stockReservationService).reserve(List.of(cartItem));
        verify(cartClient).clearCart(eq(USER_ID), any());
        verify(orderEventProducer).sendOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void checkout_WhenCartIsEmpty_ThrowsException() {
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(), BigDecimal.ZERO);
        when(cartClient.getCart(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", cart));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.checkout(USER_ID, new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.IDRAM, null)));

        assertEquals(400, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(stockReservationService, never()).reserve(any());
    }

    @Test
    void checkout_WhenStockReservationFails_PropagatesWithoutDoubleCompensating() {
        CartItemResponse item1 = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        CartItemResponse item2 = new CartItemResponse(2L, "Laptop", BigDecimal.valueOf(500), 1, BigDecimal.valueOf(500));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(item1, item2), BigDecimal.valueOf(600));

        when(cartClient.getCart(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", cart));
        doThrow(new TechShopException("Insufficient stock for product 2", 409))
                .when(stockReservationService).reserve(cart.items());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.checkout(USER_ID, new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.IDRAM, null)));

        assertEquals(409, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(stockReservationService, never()).restore(anyList());
    }

    @Test
    void checkout_WhenPaymentMethodUnsupported_CompensatesReservedStockAndThrowsBadRequest() {
        CartItemResponse cartItem = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        CartResponse cart = new CartResponse(1L, USER_ID, List.of(cartItem), BigDecimal.valueOf(100));

        when(cartClient.getCart(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", cart));
        when(orderMapper.toAddress(any(AddressRequest.class))).thenReturn(new Address());
        when(orderMapper.toEntity(any(), any(), any(), any(), any())).thenAnswer(inv -> {
            Order order = new Order();
            order.setUserId(inv.getArgument(1));
            return order;
        });
        when(orderMapper.toOrderItem(any(), any())).thenReturn(new OrderItem());
        when(paymentProviderFactory.resolve(PaymentMethod.TELCELL))
                .thenThrow(new TechShopException("Unsupported payment method: TELCELL", 400));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.checkout(USER_ID, new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.TELCELL, null)));

        assertEquals(400, ex.getStatusCode());
        verify(orderRepository, never()).save(any());
        verify(stockReservationService).restore(cart.items());
    }

    @Test
    void getOrderById_WhenOwnedByUser_ReturnsOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PENDING));

        OrderResponse result = orderService.getOrderById(USER_ID, 1L);

        assertNotNull(result);
        assertEquals(USER_ID, result.userId());
    }

    @Test
    void getOrderById_WhenNotOwnedByUser_ThrowsNotFound() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(2L);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.getOrderById(USER_ID, 1L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void getOrderTracking_WhenOwnedByUser_ReturnsStatusHistory() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.transitionTo(OrderStatus.PENDING, "Order created from cart");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toHistoryResponse(any()))
                .thenReturn(new OrderStatusHistoryResponse(OrderStatus.PENDING, "Order created from cart", LocalDateTime.now()));

        List<OrderStatusHistoryResponse> result = orderService.getOrderTracking(USER_ID, 1L);

        assertEquals(1, result.size());
        assertEquals(OrderStatus.PENDING, result.getFirst().status());
    }

    @Test
    void getOrderTracking_WhenNotOwnedByUser_ThrowsNotFound() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(2L);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.getOrderTracking(USER_ID, 1L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void getOrderByIdAdmin_WhenExists_ReturnsOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PENDING));

        OrderResponse result = orderService.getOrderByIdAdmin(1L);

        assertNotNull(result);
    }

    @Test
    void getOrderByIdAdmin_WhenNotFound_ThrowsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.getOrderByIdAdmin(1L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void payOrder_WhenPendingAndPaymentVerified_TransitionsToPaid() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.IDRAM);
        order.setPaymentReference("IDRAM-ref");
        UserResponse user = new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(paymentProviderFactory.resolve(PaymentMethod.IDRAM)).thenReturn(paymentProvider);
        when(paymentProvider.verifyPayment("IDRAM-ref")).thenReturn(new PaymentVerificationResult(PaymentStatus.PAID, "ok"));
        when(userClient.getUser(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", user));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PAID));

        OrderResponse result = orderService.payOrder(USER_ID, 1L);

        assertEquals(OrderStatus.PAID, result.status());
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());
        assertEquals(1, order.getStatusHistory().size());
        verify(orderEventProducer).sendOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void payOrder_WhenUserLookupFailsDuringNotification_StillTransitionsOrder() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.IDRAM);
        order.setPaymentReference("IDRAM-ref");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(paymentProviderFactory.resolve(PaymentMethod.IDRAM)).thenReturn(paymentProvider);
        when(paymentProvider.verifyPayment("IDRAM-ref")).thenReturn(new PaymentVerificationResult(PaymentStatus.PAID, "ok"));
        when(userClient.getUser(eq(USER_ID), any())).thenThrow(new RuntimeException("user-service unavailable"));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PAID));

        OrderResponse result = orderService.payOrder(USER_ID, 1L);

        assertEquals(OrderStatus.PAID, result.status());
        assertEquals(OrderStatus.PAID, order.getStatus());
        verify(orderEventProducer, never()).sendOrderStatusChangedEvent(any());
    }

    @Test
    void payOrder_WhenVerificationFails_ThrowsPaymentRequired() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.IDRAM);
        order.setPaymentReference("IDRAM-ref");
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
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PAID);
        order.setPaymentMethod(PaymentMethod.IDRAM);
        order.setPaymentReference("IDRAM-ref");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(paymentProviderFactory.resolve(PaymentMethod.IDRAM)).thenReturn(paymentProvider);
        when(paymentProvider.verifyPayment("IDRAM-ref")).thenReturn(new PaymentVerificationResult(PaymentStatus.PAID, "ok"));

        TechShopException ex = assertThrows(TechShopException.class, () -> orderService.payOrder(USER_ID, 1L));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void cancelOrder_WhenPending_CancelsAndRestoresStock() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        order.getItems().add(item);
        UserResponse user = new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(userClient.getUser(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", user));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.CANCELLED));

        OrderResponse result = orderService.cancelOrder(USER_ID, 1L);

        assertEquals(OrderStatus.CANCELLED, result.status());
        verify(stockReservationService).restore(order);
        verify(orderEventProducer).sendOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void cancelOrder_WhenProcessing_ThrowsException() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        TechShopException ex = assertThrows(TechShopException.class, () -> orderService.cancelOrder(USER_ID, 1L));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void updateOrderStatus_ValidTransition_Succeeds() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PAID);
        UserResponse user = new UserResponse(USER_ID, "Mariam", "mariam@test.com", UserRole.CUSTOMER, LocalDateTime.now(), true);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(userClient.getUser(eq(USER_ID), any())).thenReturn(new ApiResponse<>(true, "Success", user));
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PROCESSING));

        OrderResponse result = orderService.updateOrderStatus(1L, new OrderStatusUpdateRequest(OrderStatus.PROCESSING, "Packing"));

        assertEquals(OrderStatus.PROCESSING, result.status());
        verify(orderEventProducer).sendOrderStatusChangedEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> orderService.updateOrderStatus(1L, new OrderStatusUpdateRequest(OrderStatus.DELIVERED, null)));

        assertEquals(409, ex.getStatusCode());
    }

    @Test
    void getAllOrders_ReturnsPagedResults() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PENDING));

        PageResponse<OrderResponse> result = orderService.getAllOrders(null, null, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
    }

    @Test
    void getAllOrders_FilteredByUserId_ReturnsOnlyThatUsersOrders() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(USER_ID);
        order.setStatus(OrderStatus.PENDING);
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(page);
        when(orderMapper.toResponse(order)).thenReturn(sampleResponse(OrderStatus.PENDING));

        PageResponse<OrderResponse> result = orderService.getAllOrders(null, USER_ID, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
    }
}
