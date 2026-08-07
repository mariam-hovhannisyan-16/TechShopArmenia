package am.techshop.product.mapper;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "isNew", expression = "java(product.isNew())")
    ProductResponse toResponse(Product product);

    @Mapping(target = "new", expression = "java(request.isNew())")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "discountPercentage", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "storageOptions", ignore = true)
    @Mapping(target = "simOptions", ignore = true)
    @Mapping(target = "colorVariants", ignore = true)
    Product toEntity(ProductRequest request);
}