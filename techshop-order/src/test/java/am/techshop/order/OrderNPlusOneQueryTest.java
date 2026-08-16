package am.techshop.order;

import am.techshop.common.enums.OrderStatus;
import am.techshop.common.enums.PaymentStatus;
import am.techshop.order.entity.Address;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import am.techshop.order.repository.OrderRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderNPlusOneQueryTest {

    private static final int ORDER_COUNT = 10;
    private static final long MAX_EXPECTED_QUERIES = 6;
    private static final Long TEST_USER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(TEST_USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
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

    private void seedOrderWithItemsAndHistory() {
        Order order = new Order();
        order.setUserId(TEST_USER_ID);
        order.setTotalPrice(BigDecimal.valueOf(150));
        order.setShippingAddress(sampleAddress());
        order.setBillingAddress(sampleAddress());
        order.setPaymentStatus(PaymentStatus.PAID);

        OrderItem itemOne = new OrderItem();
        itemOne.setProductId(1L);
        itemOne.setProductName("Phone");
        itemOne.setProductPrice(BigDecimal.valueOf(100));
        itemOne.setQuantity(1);
        itemOne.setOrder(order);
        order.getItems().add(itemOne);

        OrderItem itemTwo = new OrderItem();
        itemTwo.setProductId(2L);
        itemTwo.setProductName("Case");
        itemTwo.setProductPrice(BigDecimal.valueOf(50));
        itemTwo.setQuantity(1);
        itemTwo.setOrder(order);
        order.getItems().add(itemTwo);

        order.transitionTo(OrderStatus.PENDING, "Order created from cart");
        order.transitionTo(OrderStatus.PAID, "Payment verified via IDRAM");

        orderRepository.save(order);
    }

    private Statistics statistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        return stats;
    }

    @Test
    void getUserOrders_QueryCountStaysConstantRegardlessOfOrderCount() throws Exception {
        for (int i = 0; i < ORDER_COUNT; i++) {
            seedOrderWithItemsAndHistory();
        }

        Statistics stats = statistics();
        stats.clear();

        mockMvc.perform(get("/api/orders").with(authentication(asUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(ORDER_COUNT))
                .andExpect(jsonPath("$.data[0].items.length()").value(2))
                .andExpect(jsonPath("$.data[0].statusHistory.length()").value(2));

        long queryCount = stats.getPrepareStatementCount();
        assertTrue(queryCount <= MAX_EXPECTED_QUERIES,
                "Expected a constant, small number of queries (<=" + MAX_EXPECTED_QUERIES
                        + ") regardless of order count, but saw " + queryCount
                        + " — items/statusHistory are likely being lazy-loaded per order again (N+1)");
    }

    @Test
    void getAllOrdersAdmin_QueryCountStaysConstantRegardlessOfOrderCount() throws Exception {
        for (int i = 0; i < ORDER_COUNT; i++) {
            seedOrderWithItemsAndHistory();
        }

        Statistics stats = statistics();
        stats.clear();

        mockMvc.perform(get("/api/orders/admin").with(authentication(asAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(ORDER_COUNT))
                .andExpect(jsonPath("$.data.content[0].items.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].statusHistory.length()").value(2));

        long queryCount = stats.getPrepareStatementCount();
        assertTrue(queryCount <= MAX_EXPECTED_QUERIES,
                "Expected a constant, small number of queries (<=" + MAX_EXPECTED_QUERIES
                        + ") regardless of order count, but saw " + queryCount
                        + " — items/statusHistory are likely being lazy-loaded per order again (N+1)");
    }
}
