package am.techshop.product.mapper;

import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapperImpl();

    @Test
    void toResponse_MapsIsNewFromEntity() {
        Product product = Product.builder()
                .id(1L)
                .name("iPhone 15")
                .price(BigDecimal.TEN)
                .stock(1)
                .category("Phones")
                .isNew(true)
                .build();

        ProductResponse response = mapper.toResponse(product);

        assertTrue(response.isNew());
    }
}
