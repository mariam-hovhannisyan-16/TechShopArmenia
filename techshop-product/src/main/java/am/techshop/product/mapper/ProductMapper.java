package am.techshop.product.mapper;

import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.entity.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    ProductResponse toResponse(Product product);
}