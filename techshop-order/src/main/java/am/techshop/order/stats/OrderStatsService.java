package am.techshop.order.stats;

import am.techshop.common.dto.response.AdminStatsResponse;
import am.techshop.common.dto.response.OrderStatisticsResponse;
import am.techshop.common.dto.response.TopProductResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.order.client.UserClient;
import am.techshop.order.repository.OrderItemRepository;
import am.techshop.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderStatsService {

    private static final List<OrderStatus> REVENUE_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserClient userClient;

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

    @Transactional(readOnly = true)
    public AdminStatsResponse getAdminStats() {
        OrderStatisticsResponse stats = getStatistics();

        List<TopProductResponse> topProducts = orderItemRepository.findTopSellingProducts(REVENUE_STATUSES, PageRequest.of(0, 5)).stream()
                .map(row -> new TopProductResponse((Long) row[0], (String) row[1], (Long) row[2]))
                .toList();

        return new AdminStatsResponse(
                stats.totalOrders(), stats.totalRevenue(), stats.ordersByStatus(), topProducts, fetchTotalUsers());
    }

    private long fetchTotalUsers() {
        try {
            return userClient.getUserCount().data();
        } catch (Exception ex) {
            log.warn("Failed to fetch total user count for admin stats: {}", ex.getMessage());
            return 0;
        }
    }
}
