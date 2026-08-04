package am.techshop.order.service.impl;

import am.techshop.common.dto.request.AddRepairEntryRequest;
import am.techshop.common.dto.request.UpdateNotesRequest;
import am.techshop.common.dto.response.DigitalTwinDetailResponse;
import am.techshop.common.dto.response.DigitalTwinSummaryResponse;
import am.techshop.common.dto.response.RepairEntryResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.entity.DigitalTwin;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.RepairEntry;
import am.techshop.order.repository.DigitalTwinRepository;
import am.techshop.order.repository.RepairEntryRepository;
import am.techshop.order.service.DigitalTwinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DigitalTwinServiceImpl implements DigitalTwinService {

    private static final int WARRANTY_YEARS = 2;

    private final DigitalTwinRepository digitalTwinRepository;
    private final RepairEntryRepository repairEntryRepository;

    public void createDigitalTwinsForOrder(Order order) {
        LocalDate purchaseDate = LocalDate.now();
        LocalDate warrantyEndDate = purchaseDate.plusYears(WARRANTY_YEARS);

        List<DigitalTwin> twins = order.getItems().stream()
                .map(item -> {
                    DigitalTwin twin = new DigitalTwin();
                    twin.setOrderItemId(item.getId());
                    twin.setUserId(order.getUserId());
                    twin.setProductId(item.getProductId());
                    twin.setProductName(item.getProductName());
                    twin.setPurchaseDate(purchaseDate);
                    twin.setWarrantyEndDate(warrantyEndDate);
                    return twin;
                })
                .toList();

        digitalTwinRepository.saveAll(twins);
    }

    @Transactional(readOnly = true)
    public List<DigitalTwinSummaryResponse> getMyDigitalTwins(Long userId) {
        return digitalTwinRepository.findByUserIdOrderByPurchaseDateDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public DigitalTwinDetailResponse getDigitalTwinDetail(Long userId, Long id) {
        return toDetail(getOwnedDigitalTwin(userId, id));
    }

    public DigitalTwinDetailResponse addRepairEntry(Long userId, Long id, AddRepairEntryRequest request) {
        DigitalTwin twin = getOwnedDigitalTwin(userId, id);

        RepairEntry entry = new RepairEntry();
        entry.setDigitalTwinId(twin.getId());
        entry.setDescription(request.description());
        entry.setEntryDate(request.date());
        entry.setCreatedAt(LocalDateTime.now());
        repairEntryRepository.save(entry);

        return toDetail(twin);
    }

    public DigitalTwinDetailResponse updateNotes(Long userId, Long id, UpdateNotesRequest request) {
        DigitalTwin twin = getOwnedDigitalTwin(userId, id);
        twin.setNotes(request.notes());
        digitalTwinRepository.save(twin);
        return toDetail(twin);
    }

    private DigitalTwin getOwnedDigitalTwin(Long userId, Long id) {
        DigitalTwin twin = digitalTwinRepository.findById(id)
                .orElseThrow(() -> new TechShopException("Digital twin not found", 404));
        if (!twin.getUserId().equals(userId)) {
            throw new TechShopException("Digital twin not found", 404);
        }
        return twin;
    }

    private DigitalTwinSummaryResponse toSummary(DigitalTwin twin) {
        return new DigitalTwinSummaryResponse(
                twin.getId(), twin.getProductName(), twin.getPurchaseDate(),
                twin.getWarrantyEndDate(), isWarrantyActive(twin.getWarrantyEndDate()));
    }

    private DigitalTwinDetailResponse toDetail(DigitalTwin twin) {
        List<RepairEntryResponse> repairs = repairEntryRepository.findByDigitalTwinIdOrderByEntryDateDesc(twin.getId())
                .stream()
                .map(entry -> new RepairEntryResponse(entry.getId(), entry.getDescription(), entry.getEntryDate(), entry.getCreatedAt()))
                .toList();

        return new DigitalTwinDetailResponse(
                twin.getId(), twin.getProductName(), twin.getProductId(),
                twin.getPurchaseDate(), twin.getWarrantyEndDate(), isWarrantyActive(twin.getWarrantyEndDate()),
                twin.getNotes(), repairs);
    }

    private boolean isWarrantyActive(LocalDate warrantyEndDate) {
        return !warrantyEndDate.isBefore(LocalDate.now());
    }
}
