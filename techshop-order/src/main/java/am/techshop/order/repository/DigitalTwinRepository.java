package am.techshop.order.repository;

import am.techshop.order.entity.DigitalTwin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DigitalTwinRepository extends JpaRepository<DigitalTwin, Long> {

    List<DigitalTwin> findByUserIdOrderByPurchaseDateDesc(Long userId);
}
