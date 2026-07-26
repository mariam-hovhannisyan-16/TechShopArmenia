package am.techshop.order.stock;

import am.techshop.common.dto.request.StockAdjustmentRequest;
import am.techshop.common.dto.response.CartItemResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.client.ProductClient;
import am.techshop.order.entity.Order;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final ProductClient productClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public void reserve(List<CartItemResponse> items) {
        List<CartItemResponse> reserved = new ArrayList<>();
        try {
            for (CartItemResponse item : items) {
                adjustStock(item.productId(), -item.quantity());
                reserved.add(item);
            }
        } catch (TechShopException ex) {
            reserved.forEach(item -> adjustStockQuietly(item.productId(), item.quantity()));
            throw ex;
        }
    }

    public void restore(List<CartItemResponse> items) {
        items.forEach(item -> adjustStockQuietly(item.productId(), item.quantity()));
    }

    public void restore(Order order) {
        order.getItems().forEach(item -> adjustStockQuietly(item.getProductId(), item.getQuantity()));
    }

    private void adjustStock(Long productId, int delta) {
        try {
            productClient.adjustStock(productId, new StockAdjustmentRequest(delta), internalApiKey);
        } catch (FeignException.Conflict ex) {
            throw new TechShopException("Insufficient stock for product %s".formatted(productId), 409);
        } catch (FeignException ex) {
            throw new TechShopException("Product service unavailable", 503);
        }
    }

    private void adjustStockQuietly(Long productId, int delta) {
        try {
            productClient.adjustStock(productId, new StockAdjustmentRequest(delta), internalApiKey);
        } catch (FeignException ignored) {

        }
    }
}
