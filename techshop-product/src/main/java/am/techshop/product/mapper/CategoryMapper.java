package am.techshop.product.mapper;

import am.techshop.common.dto.response.CategoryResponse;
import am.techshop.product.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);

    @Mapping(target = "name", source = "name")
    @Mapping(target = "id", ignore = true)
    Category toEntity(String name);
}
