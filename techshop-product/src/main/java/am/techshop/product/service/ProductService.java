package am.techshop.product.service;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.PricePredictionResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.SurpriseBoxResponse;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    ProductResponse addProduct(ProductRequest request);
    PageResponse<ProductResponse> getAllProducts(String category, String search, int page, int size);
    ProductResponse getProductById(Long id);
    List<ProductResponse> getProductsByIds(List<Long> ids);
    void deleteProduct(Long id);
    ProductResponse adjustStock(Long id, int quantityDelta);
    ProductResponse updatePrice(Long id, BigDecimal price);
    ProductResponse updateDiscount(Long id, Integer discountPercentage);
    PricePredictionResponse getPricePrediction(Long id);
    SurpriseBoxResponse getSurpriseBox(BigDecimal budget);
}
