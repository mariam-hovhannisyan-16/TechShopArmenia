package am.techshop.product.mapper;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    @Test
    void toResponse_MapsIsNewFromEntity() {
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(BigDecimal.TEN);
        product.setStock(1);
        product.setCategory("Phones");
        product.setNew(true);

        ProductResponse response = mapper.toResponse(product);

        assertTrue(response.isNew());
    }

    @Test
    void toEntity_MapsIsNewFromRequest() {
        ProductRequest request = new ProductRequest("iPhone 15", "Desc", BigDecimal.TEN, 1, "Phones", "img.jpg", true);

        Product product = mapper.toEntity(request);

        assertEquals("iPhone 15", product.getName());
        assertTrue(product.isNew());
    }

    @Test
    void toResponse_MapsRatingAndReviewCountFromEntity() {
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 15");
        product.setPrice(BigDecimal.TEN);
        product.setStock(1);
        product.setCategory("Phones");
        product.setRating(new BigDecimal("4.33"));
        product.setReviewCount(3);

        ProductResponse response = mapper.toResponse(product);

        assertEquals(0, new BigDecimal("4.33").compareTo(response.rating()));
        assertEquals(3, response.reviewCount());
    }

    @Test
    void toEntity_IgnoresRatingAndReviewCountFromRequest() {
        ProductRequest request = new ProductRequest("iPhone 15", "Desc", BigDecimal.TEN, 1, "Phones", "img.jpg", true);

        Product product = mapper.toEntity(request);

        assertEquals(0, BigDecimal.ZERO.compareTo(product.getRating()));
        assertEquals(0, product.getReviewCount());
    }
}
