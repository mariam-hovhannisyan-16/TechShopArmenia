package am.techshop.common.dto.response;

import am.techshop.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AdminStatsResponse(
        long totalOrders,
        BigDecimal totalRevenue,
        Map<OrderStatus, Long> ordersByStatus,
        List<TopProductResponse> topProducts,
        long totalUsers
) {}
