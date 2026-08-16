package am.techshop.order;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.order.client.UserClient;
import am.techshop.order.entity.Address;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import am.techshop.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminStatsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private UserClient userClient;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private static Address sampleAddress() {
        Address address = new Address();
        address.setFullName("Mariam A");
        address.setPhone("+374000000");
        address.setLine1("1 Main St");
        address.setCity("Yerevan");
        address.setPostalCode("0001");
        address.setCountry("Armenia");
        return address;
    }

    private void saveOrder(Long userId, OrderStatus status, BigDecimal totalPrice, List<OrderItem> items) {
        Order order = new Order();
        order.setUserId(userId);
        order.setTotalPrice(totalPrice);
        order.setStatus(status);
        order.setShippingAddress(sampleAddress());
        order.setBillingAddress(sampleAddress());
        order.setPaymentStatus(PaymentStatus.PAID);
        items.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });
        orderRepository.save(order);
    }

    private static OrderItem item(Long productId, String productName, BigDecimal price, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setProductName(productName);
        item.setProductPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    @Test
    void getStats_AggregatesRealOrderData() throws Exception {
        saveOrder(1L, OrderStatus.PAID, BigDecimal.valueOf(300),
                List.of(item(1L, "Phone", BigDecimal.valueOf(100), 3)));
        saveOrder(1L, OrderStatus.PAID, BigDecimal.valueOf(200),
                List.of(item(1L, "Phone", BigDecimal.valueOf(100), 2)));
        saveOrder(2L, OrderStatus.PAID, BigDecimal.valueOf(400),
                List.of(item(2L, "Laptop", BigDecimal.valueOf(100), 4)));
        saveOrder(2L, OrderStatus.PENDING, BigDecimal.valueOf(50),
                List.of(item(2L, "Laptop", BigDecimal.valueOf(50), 1)));

        when(userClient.getUserCount()).thenReturn(new ApiResponse<>(true, "Success", 42L));

        mockMvc.perform(get("/api/admin/stats").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(4))
                .andExpect(jsonPath("$.data.totalRevenue").value(900))
                .andExpect(jsonPath("$.data.ordersByStatus.PAID").value(3))
                .andExpect(jsonPath("$.data.ordersByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.data.topProducts[0].productName").value("Phone"))
                .andExpect(jsonPath("$.data.topProducts[0].quantitySold").value(5))
                .andExpect(jsonPath("$.data.topProducts[1].productName").value("Laptop"))
                .andExpect(jsonPath("$.data.topProducts[1].quantitySold").value(4))
                .andExpect(jsonPath("$.data.totalUsers").value(42));
    }

    @Test
    void getStats_WhenUserServiceUnavailable_StillReturnsOrderData() throws Exception {
        saveOrder(1L, OrderStatus.PAID, BigDecimal.valueOf(100), List.of(item(1L, "Phone", BigDecimal.valueOf(100), 1)));
        when(userClient.getUserCount()).thenThrow(new RuntimeException("user-service unavailable"));

        mockMvc.perform(get("/api/admin/stats").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.totalUsers").value(0));
    }

    @Test
    void getStats_AsRegularUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/stats").with(authentication(asUser())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getStats_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }
}
