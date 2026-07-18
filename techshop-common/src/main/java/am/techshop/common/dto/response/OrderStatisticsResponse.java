package am.techshop.common.dto.response;

import am.techshop.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public record OrderStatisticsResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        BigDecimal averageOrderValue,
        Map<OrderStatus, Long> ordersByStatus
) {}
