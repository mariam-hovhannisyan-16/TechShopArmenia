package am.techshop.order.stock;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.client.ProductClient;
import am.techshop.order.entity.Order;
import am.techshop.order.entity.OrderItem;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private StockReservationService stockReservationService;

    private static FeignException.Conflict conflict() {
        Request request = Request.create(Request.HttpMethod.PATCH, "/api/products/1/stock",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(409).reason("Conflict").request(request).build();
        return (FeignException.Conflict) FeignException.errorStatus("ProductClient#adjustStock", response);
    }

    @Test
    void reserve_WhenAllItemsHaveStock_AdjustsEachOnce() {
        CartItemResponse item1 = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        CartItemResponse item2 = new CartItemResponse(2L, "Laptop", BigDecimal.valueOf(500), 1, BigDecimal.valueOf(500));

        when(productClient.adjustStock(eq(1L), any(), any())).thenReturn(new ApiResponse<>(true, "ok", null));
        when(productClient.adjustStock(eq(2L), any(), any())).thenReturn(new ApiResponse<>(true, "ok", null));

        stockReservationService.reserve(List.of(item1, item2));

        verify(productClient, times(1)).adjustStock(eq(1L), any(), any());
        verify(productClient, times(1)).adjustStock(eq(2L), any(), any());
    }

    @Test
    void reserve_WhenSecondItemInsufficientStock_CompensatesFirstItemAndThrows() {
        CartItemResponse item1 = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        CartItemResponse item2 = new CartItemResponse(2L, "Laptop", BigDecimal.valueOf(500), 1, BigDecimal.valueOf(500));

        when(productClient.adjustStock(eq(1L), any(), any())).thenReturn(new ApiResponse<>(true, "ok", null));
        when(productClient.adjustStock(eq(2L), any(), any())).thenThrow(conflict());

        TechShopException ex = assertThrows(TechShopException.class,
                () -> stockReservationService.reserve(List.of(item1, item2)));

        assertEquals(409, ex.getStatusCode());
        verify(productClient, times(2)).adjustStock(eq(1L), any(), any());
        verify(productClient, times(1)).adjustStock(eq(2L), any(), any());
    }

    @Test
    void reserve_WhenProductServiceUnavailable_ThrowsServiceUnavailable() {
        CartItemResponse item = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 1, BigDecimal.valueOf(100));
        Request request = Request.create(Request.HttpMethod.PATCH, "/api/products/1/stock",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(503).reason("Unavailable").request(request).build();

        when(productClient.adjustStock(eq(1L), any(), any()))
                .thenThrow(FeignException.errorStatus("ProductClient#adjustStock", response));

        TechShopException ex = assertThrows(TechShopException.class,
                () -> stockReservationService.reserve(List.of(item)));

        assertEquals(503, ex.getStatusCode());
    }

    @Test
    void restoreItems_AdjustsEachItemBackUp() {
        CartItemResponse item1 = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200));

        stockReservationService.restore(List.of(item1));

        verify(productClient).adjustStock(eq(1L), any(), any());
    }

    @Test
    void restoreItems_WhenProductServiceUnavailable_SwallowsException() {
        CartItemResponse item1 = new CartItemResponse(1L, "Phone", BigDecimal.valueOf(100), 2, BigDecimal.valueOf(200));
        Request request = Request.create(Request.HttpMethod.PATCH, "/api/products/1/stock",
                Map.of(), null, StandardCharsets.UTF_8, null);
        Response response = Response.builder().status(503).reason("Unavailable").request(request).build();
        when(productClient.adjustStock(eq(1L), any(), any()))
                .thenThrow(FeignException.errorStatus("ProductClient#adjustStock", response));

        stockReservationService.restore(List.of(item1));

        verify(productClient).adjustStock(eq(1L), any(), any());
    }

    @Test
    void restoreOrder_AdjustsEachOrderItemBackUp() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setProductId(1L);
        item.setQuantity(2);
        order.getItems().add(item);

        stockReservationService.restore(order);

        verify(productClient).adjustStock(eq(1L), any(), any());
    }
}
