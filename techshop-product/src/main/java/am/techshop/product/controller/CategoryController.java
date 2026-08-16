package am.techshop.product.controller;

import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.CategoryResponse;
import am.techshop.product.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Browse product categories")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/api/categories")
    @Operation(summary = "List all product categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAllCategories()));
    }
}
