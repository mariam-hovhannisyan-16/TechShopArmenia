package am.techshop.order.service;

import am.techshop.common.dto.request.AddRepairEntryRequest;
import am.techshop.common.dto.request.UpdateNotesRequest;
import am.techshop.common.dto.response.DigitalTwinDetailResponse;
import am.techshop.common.dto.response.DigitalTwinSummaryResponse;
import am.techshop.order.entity.Order;

import java.util.List;

public interface DigitalTwinService {

    void createDigitalTwinsForOrder(Order order);

    List<DigitalTwinSummaryResponse> getMyDigitalTwins(Long userId);

    DigitalTwinDetailResponse getDigitalTwinDetail(Long userId, Long id);

    DigitalTwinDetailResponse addRepairEntry(Long userId, Long id, AddRepairEntryRequest request);

    DigitalTwinDetailResponse updateNotes(Long userId, Long id, UpdateNotesRequest request);
}
