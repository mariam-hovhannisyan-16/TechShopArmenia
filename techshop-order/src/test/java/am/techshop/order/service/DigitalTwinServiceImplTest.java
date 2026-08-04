package am.techshop.order.service;

import am.techshop.common.dto.request.AddRepairEntryRequest;
import am.techshop.common.dto.request.UpdateNotesRequest;
import am.techshop.common.dto.response.DigitalTwinDetailResponse;
import am.techshop.common.dto.response.DigitalTwinSummaryResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.entity.DigitalTwin;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import am.techshop.order.entity.RepairEntry;
import am.techshop.order.repository.DigitalTwinRepository;
import am.techshop.order.repository.RepairEntryRepository;
import am.techshop.order.service.impl.DigitalTwinServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DigitalTwinServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private DigitalTwinRepository digitalTwinRepository;

    @Mock
    private RepairEntryRepository repairEntryRepository;

    @InjectMocks
    private DigitalTwinServiceImpl digitalTwinService;

    private OrderItem item(long id, long productId, String name) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setProductId(productId);
        item.setProductName(name);
        item.setQuantity(1);
        return item;
    }

    @Test
    void createDigitalTwinsForOrder_CreatesOneTwinPerOrderItem_WithWarrantyTwoYearsOut() {
        Order order = new Order();
        order.setUserId(USER_ID);
        order.getItems().add(item(10L, 100L, "Phone"));
        order.getItems().add(item(11L, 200L, "Laptop"));

        digitalTwinService.createDigitalTwinsForOrder(order);

        ArgumentCaptor<List<DigitalTwin>> captor = ArgumentCaptor.forClass(List.class);
        verify(digitalTwinRepository).saveAll(captor.capture());
        List<DigitalTwin> twins = captor.getValue();

        assertEquals(2, twins.size());
        LocalDate today = LocalDate.now();
        for (DigitalTwin twin : twins) {
            assertEquals(USER_ID, twin.getUserId());
            assertEquals(today, twin.getPurchaseDate());
            assertEquals(today.plusYears(2), twin.getWarrantyEndDate());
        }
        assertEquals(10L, twins.get(0).getOrderItemId());
        assertEquals(100L, twins.get(0).getProductId());
        assertEquals("Phone", twins.get(0).getProductName());
        assertEquals(11L, twins.get(1).getOrderItemId());
        assertEquals(200L, twins.get(1).getProductId());
        assertEquals("Laptop", twins.get(1).getProductName());
    }

    @Test
    void createDigitalTwinsForOrder_WhenOrderHasNoItems_SavesEmptyList() {
        Order order = new Order();
        order.setUserId(USER_ID);

        digitalTwinService.createDigitalTwinsForOrder(order);

        verify(digitalTwinRepository).saveAll(List.of());
    }

    @Test
    void getMyDigitalTwins_ReflectsActiveAndExpiredWarranty() {
        DigitalTwin active = new DigitalTwin(1L, 10L, USER_ID, 100L, "Phone",
                LocalDate.now().minusMonths(6), LocalDate.now().plusMonths(18), null);
        DigitalTwin expired = new DigitalTwin(2L, 11L, USER_ID, 200L, "Old Laptop",
                LocalDate.now().minusYears(3), LocalDate.now().minusYears(1), null);
        when(digitalTwinRepository.findByUserIdOrderByPurchaseDateDesc(USER_ID)).thenReturn(List.of(active, expired));

        List<DigitalTwinSummaryResponse> result = digitalTwinService.getMyDigitalTwins(USER_ID);

        assertEquals(2, result.size());
        assertTrue(result.get(0).warrantyActive());
        assertFalse(result.get(1).warrantyActive());
    }

    @Test
    void getDigitalTwinDetail_WhenOwnedByUser_ReturnsDetailWithRepairs() {
        DigitalTwin twin = new DigitalTwin(1L, 10L, USER_ID, 100L, "Phone",
                LocalDate.now(), LocalDate.now().plusYears(2), "Handle with care");
        RepairEntry repair = new RepairEntry(5L, 1L, "Screen replaced", LocalDate.now(), LocalDateTime.now());
        when(digitalTwinRepository.findById(1L)).thenReturn(Optional.of(twin));
        when(repairEntryRepository.findByDigitalTwinIdOrderByEntryDateDesc(1L)).thenReturn(List.of(repair));

        DigitalTwinDetailResponse result = digitalTwinService.getDigitalTwinDetail(USER_ID, 1L);

        assertEquals("Phone", result.productName());
        assertEquals("Handle with care", result.notes());
        assertEquals(1, result.repairs().size());
        assertEquals("Screen replaced", result.repairs().get(0).description());
    }

    @Test
    void getDigitalTwinDetail_WhenNotFound_ThrowsNotFound() {
        when(digitalTwinRepository.findById(99L)).thenReturn(Optional.empty());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> digitalTwinService.getDigitalTwinDetail(USER_ID, 99L));

        assertEquals(404, ex.getStatusCode());
    }

    @Test
    void getDigitalTwinDetail_WhenOwnedByAnotherUser_ThrowsNotFound() {
        DigitalTwin twin = new DigitalTwin(1L, 10L, 999L, 100L, "Phone",
                LocalDate.now(), LocalDate.now().plusYears(2), null);
        when(digitalTwinRepository.findById(1L)).thenReturn(Optional.of(twin));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> digitalTwinService.getDigitalTwinDetail(USER_ID, 1L));

        assertEquals(404, ex.getStatusCode());
        verify(repairEntryRepository, never()).findByDigitalTwinIdOrderByEntryDateDesc(any());
    }

    @Test
    void addRepairEntry_WhenOwnedByUser_SavesEntryAndReturnsUpdatedDetail() {
        DigitalTwin twin = new DigitalTwin(1L, 10L, USER_ID, 100L, "Phone",
                LocalDate.now(), LocalDate.now().plusYears(2), null);
        AddRepairEntryRequest request = new AddRepairEntryRequest("Battery replaced", LocalDate.now());
        when(digitalTwinRepository.findById(1L)).thenReturn(Optional.of(twin));
        when(repairEntryRepository.findByDigitalTwinIdOrderByEntryDateDesc(1L)).thenReturn(List.of());

        digitalTwinService.addRepairEntry(USER_ID, 1L, request);

        ArgumentCaptor<RepairEntry> captor = ArgumentCaptor.forClass(RepairEntry.class);
        verify(repairEntryRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getDigitalTwinId());
        assertEquals("Battery replaced", captor.getValue().getDescription());
    }

    @Test
    void addRepairEntry_WhenOwnedByAnotherUser_ThrowsNotFoundAndDoesNotSave() {
        DigitalTwin twin = new DigitalTwin(1L, 10L, 999L, 100L, "Phone",
                LocalDate.now(), LocalDate.now().plusYears(2), null);
        when(digitalTwinRepository.findById(1L)).thenReturn(Optional.of(twin));

        assertThrows(TechShopException.class, () ->
                digitalTwinService.addRepairEntry(USER_ID, 1L, new AddRepairEntryRequest("x", LocalDate.now())));

        verify(repairEntryRepository, never()).save(any());
    }

    @Test
    void updateNotes_WhenOwnedByUser_UpdatesNotes() {
        DigitalTwin twin = new DigitalTwin(1L, 10L, USER_ID, 100L, "Phone",
                LocalDate.now(), LocalDate.now().plusYears(2), null);
        when(digitalTwinRepository.findById(1L)).thenReturn(Optional.of(twin));
        when(repairEntryRepository.findByDigitalTwinIdOrderByEntryDateDesc(1L)).thenReturn(List.of());

        DigitalTwinDetailResponse result = digitalTwinService.updateNotes(USER_ID, 1L, new UpdateNotesRequest("Bought as a gift"));

        assertEquals("Bought as a gift", twin.getNotes());
        assertEquals("Bought as a gift", result.notes());
        verify(digitalTwinRepository).save(twin);
    }

    @Test
    void updateNotes_WhenOwnedByAnotherUser_ThrowsNotFoundAndDoesNotSave() {
        DigitalTwin twin = new DigitalTwin(1L, 10L, 999L, 100L, "Phone",
                LocalDate.now(), LocalDate.now().plusYears(2), null);
        when(digitalTwinRepository.findById(1L)).thenReturn(Optional.of(twin));

        assertThrows(TechShopException.class, () ->
                digitalTwinService.updateNotes(USER_ID, 1L, new UpdateNotesRequest("hijacked")));

        verify(digitalTwinRepository, never()).save(any());
    }
}
