package am.techshop.order.repository;

import am.techshop.common.enums.OrderStatus;
import am.techshop.order.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi.productId, oi.productName, SUM(oi.quantity) " +
            "FROM OrderItem oi WHERE oi.order.status IN :statuses " +
            "GROUP BY oi.productId, oi.productName ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts(@Param("statuses") List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
            "WHERE oi.order.userId = :userId AND oi.productId = :productId AND oi.order.status IN :statuses")
    boolean existsVerifiedPurchase(@Param("userId") Long userId, @Param("productId") Long productId,
                                    @Param("statuses") List<OrderStatus> statuses);
}
