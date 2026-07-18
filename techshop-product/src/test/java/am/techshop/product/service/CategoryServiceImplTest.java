package am.techshop.product.service;

import am.techshop.common.dto.response.CategoryResponse;
import am.techshop.product.entity.Category;
import am.techshop.product.mapper.CategoryMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void getAllCategories_ReturnsAllCategories() {
        Category category = Category.builder().id(1L).name("Phones").build();
        CategoryResponse response = new CategoryResponse(1L, "Phones");

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(response);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertEquals(1, result.size());
        assertEquals("Phones", result.get(0).name());
    }
}
