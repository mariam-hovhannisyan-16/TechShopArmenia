package am.techshop.product.mapper;

import am.techshop.common.dto.response.CategoryResponse;
import am.techshop.product.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}
