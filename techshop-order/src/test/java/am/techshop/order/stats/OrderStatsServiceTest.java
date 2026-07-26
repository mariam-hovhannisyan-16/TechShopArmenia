package am.techshop.order.stats;

import am.techshop.common.dto.response.AdminStatsResponse;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.OrderStatisticsResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.order.client.UserClient;
import am.techshop.order.repository.OrderItemRepository;
import am.techshop.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderStatsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private OrderStatsService orderStatsService;

    @Test
    void getStatistics_ReturnsAggregates() {
        when(orderRepository.count()).thenReturn(5L);
        when(orderRepository.sumTotalPriceByStatusIn(any())).thenReturn(BigDecimal.valueOf(1000));
        when(orderRepository.countByStatusIn(any())).thenReturn(4L);
        when(orderRepository.countGroupedByStatus()).thenReturn(
                List.of(new Object[]{OrderStatus.PENDING, 1L}, new Object[]{OrderStatus.PAID, 4L}));

        OrderStatisticsResponse result = orderStatsService.getStatistics();

        assertEquals(5L, result.totalOrders());
        assertEquals(BigDecimal.valueOf(1000), result.totalRevenue());
        assertEquals(BigDecimal.valueOf(250.00).setScale(2, RoundingMode.HALF_UP), result.averageOrderValue());
    }

    @Test
    void getStatistics_WhenNoRevenueOrders_AvoidsDivisionByZero() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumTotalPriceByStatusIn(any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countByStatusIn(any())).thenReturn(0L);
        when(orderRepository.countGroupedByStatus()).thenReturn(List.of());

        OrderStatisticsResponse result = orderStatsService.getStatistics();

        assertEquals(0L, result.totalOrders());
        assertEquals(BigDecimal.ZERO, result.totalRevenue());
        assertEquals(BigDecimal.ZERO, result.averageOrderValue());
    }

    @Test
    void getAdminStats_CombinesStatisticsTopProductsAndUserCount() {
        when(orderRepository.count()).thenReturn(2L);
        when(orderRepository.sumTotalPriceByStatusIn(any())).thenReturn(BigDecimal.valueOf(500));
        when(orderRepository.countByStatusIn(any())).thenReturn(2L);
        when(orderRepository.countGroupedByStatus()).thenReturn(List.<Object[]>of(new Object[]{OrderStatus.PAID, 2L}));
        when(orderItemRepository.findTopSellingProducts(any(), any(Pageable.class))).thenReturn(
                List.<Object[]>of(new Object[]{1L, "Phone", 5L}));
        when(userClient.getUserCount()).thenReturn(new ApiResponse<>(true, "Success", 42L));

        AdminStatsResponse result = orderStatsService.getAdminStats();

        assertEquals(2L, result.totalOrders());
        assertEquals(1, result.topProducts().size());
        assertEquals("Phone", result.topProducts().getFirst().productName());
        assertEquals(42L, result.totalUsers());
    }

    @Test
    void getAdminStats_WhenUserServiceUnavailable_DefaultsUserCountToZero() {
        when(orderRepository.count()).thenReturn(0L);
        when(orderRepository.sumTotalPriceByStatusIn(any())).thenReturn(BigDecimal.ZERO);
        when(orderRepository.countByStatusIn(any())).thenReturn(0L);
        when(orderRepository.countGroupedByStatus()).thenReturn(List.of());
        when(orderItemRepository.findTopSellingProducts(any(), any(Pageable.class))).thenReturn(List.of());
        when(userClient.getUserCount()).thenThrow(new RuntimeException("user-service unavailable"));

        AdminStatsResponse result = orderStatsService.getAdminStats();

        assertEquals(0L, result.totalUsers());
    }
}
