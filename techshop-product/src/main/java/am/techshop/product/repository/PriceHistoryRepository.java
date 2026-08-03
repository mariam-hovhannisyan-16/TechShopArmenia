package am.techshop.product.repository;

import am.techshop.product.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProductIdAndChangedAtAfterOrderByChangedAtAsc(Long productId, LocalDateTime since);

    List<PriceHistory> findByProductIdOrderByChangedAtAsc(Long productId);
}
