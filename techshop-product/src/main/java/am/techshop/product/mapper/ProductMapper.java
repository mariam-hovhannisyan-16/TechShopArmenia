package am.techshop.product.mapper;

import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Explicit mapping needed: standard JavaBean introspection derives the
    // property name "new" (not "isNew") from a boolean getter already named
    // isNew(), so MapStruct can't auto-match it against the isNew record
    // component without this.
    @Mapping(target = "isNew", expression = "java(product.isNew())")
    ProductResponse toResponse(Product product);
}