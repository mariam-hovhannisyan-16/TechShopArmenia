package am.techshop.product.service;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;

public interface ProductService {
    ProductResponse addProduct(ProductRequest request);
    PageResponse<ProductResponse> getAllProducts(String category, String search, int page, int size);
    ProductResponse getProductById(Long id);
    void deleteProduct(Long id);
    ProductResponse adjustStock(Long id, int quantityDelta);
}
