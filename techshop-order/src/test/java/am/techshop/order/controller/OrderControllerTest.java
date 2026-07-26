package am.techshop.order.controller;

import am.techshop.common.dto.request.AddressRequest;
import am.techshop.common.dto.request.CheckoutRequest;
import am.techshop.common.dto.request.OrderStatusUpdateRequest;
import am.techshop.common.dto.response.OrderResponse;
import am.techshop.common.dto.response.OrderStatisticsResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentMethod;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.order.config.SecurityConfig;
import am.techshop.order.security.JwtAuthFilter;
import am.techshop.order.security.JwtService;
import am.techshop.order.service.OrderService;
import am.techshop.order.stats.OrderStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private OrderStatsService orderStatsService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static AddressRequest sampleAddress() {
        return new AddressRequest("Mariam A", "+374000000", "1 Main St", null, "Yerevan", null, "0001", "Armenia");
    }

    private static OrderResponse sampleOrder(OrderStatus status) {
        return new OrderResponse(1L, USER_ID, List.of(), BigDecimal.valueOf(200), status,
                null, null, null, List.of(), PaymentMethod.IDRAM, "IDRAM-ref", PaymentStatus.PENDING, null, null, null, null);
    }

    @Test
    void checkout_ReturnsCreatedOrder() throws Exception {
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), "Ring the bell", PaymentMethod.IDRAM, null);
        when(orderService.checkout(USER_ID, request)).thenReturn(sampleOrder(OrderStatus.PENDING));

        mockMvc.perform(post("/api/orders/checkout")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Order created"));
    }

    @Test
    void checkout_AsAdmin_ReturnsForbidden() throws Exception {
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.IDRAM, null);

        mockMvc.perform(post("/api/orders/checkout")
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(orderService, never()).checkout(any(), any());
    }

    @Test
    void checkout_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), null, PaymentMethod.IDRAM, null);

        mockMvc.perform(post("/api/orders/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkout_WithInvalidAddress_ReturnsBadRequest() throws Exception {
        AddressRequest blankAddress = new AddressRequest("", "", "", null, "", null, "", "");
        CheckoutRequest request = new CheckoutRequest(blankAddress, blankAddress, null, PaymentMethod.IDRAM, null);

        mockMvc.perform(post("/api/orders/checkout")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkout_WithoutPaymentMethod_ReturnsBadRequest() throws Exception {
        CheckoutRequest request = new CheckoutRequest(sampleAddress(), sampleAddress(), null, null, null);

        mockMvc.perform(post("/api/orders/checkout")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserOrders_ReturnsOrderList() throws Exception {
        when(orderService.getUserOrders(USER_ID)).thenReturn(List.of(sampleOrder(OrderStatus.PENDING)));

        mockMvc.perform(get("/api/orders").with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(USER_ID));
    }

    @Test
    void payOrder_ReturnsUpdatedOrder() throws Exception {
        when(orderService.payOrder(USER_ID, 1L)).thenReturn(sampleOrder(OrderStatus.PAID));

        mockMvc.perform(patch("/api/orders/{id}/pay", 1L).with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order paid"));
    }

    @Test
    void cancelOrder_ReturnsUpdatedOrder() throws Exception {
        when(orderService.cancelOrder(USER_ID, 1L)).thenReturn(sampleOrder(OrderStatus.CANCELLED));

        mockMvc.perform(patch("/api/orders/{id}/cancel", 1L).with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order cancelled"));
    }

    @Test
    void adminEndpoint_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/orders/admin").with(authentication(asUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_WhenCalledByAdmin_ReturnsPagedOrders() throws Exception {
        PageResponse<OrderResponse> page = new PageResponse<>(List.of(sampleOrder(OrderStatus.PENDING)), 0, 20, 1, 1);
        when(orderService.getAllOrders(null, null, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/orders/admin").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(USER_ID));
    }

    @Test
    void adminEndpoint_FilteredByUserId_ReturnsOnlyThatUsersOrders() throws Exception {
        PageResponse<OrderResponse> page = new PageResponse<>(List.of(sampleOrder(OrderStatus.PENDING)), 0, 20, 1, 1);
        when(orderService.getAllOrders(null, USER_ID, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/orders/admin").param("userId", USER_ID.toString()).with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].userId").value(USER_ID));
    }

    @Test
    void adminEndpoint_FilteredByUserId_WhenCalledByRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/orders/admin").param("userId", USER_ID.toString()).with(authentication(asUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOrderStatus_WhenCalledByAdmin_ReturnsUpdatedOrder() throws Exception {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.PROCESSING, "Packing started");
        when(orderService.updateOrderStatus(1L, request)).thenReturn(sampleOrder(OrderStatus.PROCESSING));

        mockMvc.perform(patch("/api/orders/admin/{id}/status", 1L)
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    void getStatistics_WhenCalledByAdmin_ReturnsStatistics() throws Exception {
        OrderStatisticsResponse stats = new OrderStatisticsResponse(10L, BigDecimal.valueOf(5000),
                BigDecimal.valueOf(500), Map.of(OrderStatus.PENDING, 2L, OrderStatus.PAID, 8L));
        when(orderStatsService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/orders/admin/statistics").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(10));
    }
}
