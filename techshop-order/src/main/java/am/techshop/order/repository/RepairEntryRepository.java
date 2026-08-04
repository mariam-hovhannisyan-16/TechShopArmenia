package am.techshop.order.repository;

import am.techshop.order.entity.RepairEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RepairEntryRepository extends JpaRepository<RepairEntry, Long> {

    List<RepairEntry> findByDigitalTwinIdOrderByEntryDateDesc(Long digitalTwinId);
}
