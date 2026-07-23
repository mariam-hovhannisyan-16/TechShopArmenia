package am.techshop.product.mapper;

import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "isNew", expression = "java(product.isNew())")
    ProductResponse toResponse(Product product);
}