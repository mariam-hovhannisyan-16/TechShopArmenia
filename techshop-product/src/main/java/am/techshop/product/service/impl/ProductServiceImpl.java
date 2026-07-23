package am.techshop.product.service.impl;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.exception.ProductNotFoundException;
import am.techshop.common.exception.TechShopException;
import am.techshop.product.entity.Product;
import am.techshop.product.mapper.ProductMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.repository.ProductRepository;
import am.techshop.product.repository.ProductSpecifications;
import am.techshop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductResponse addProduct(ProductRequest request) {
        if (!categoryRepository.existsByNameIgnoreCase(request.category())) {
            throw new TechShopException("Unknown category: " + request.category(), 400);
        }

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .stock(request.stock())
                .category(request.category())
                .imageUrl(request.imageUrl())
                .isNew(request.isNew())
                .build();
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAllProducts(String category, String search, int page, int size) {
        Specification<Product> spec = Specification
                .where(ProductSpecifications.hasCategory(category))
                .and(ProductSpecifications.matchesSearch(search));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> result = productRepository.findAll(spec, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(productMapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    public ProductResponse adjustStock(Long id, int quantityDelta) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        int newStock = product.getStock() + quantityDelta;
        if (newStock < 0) {
            throw new TechShopException("Insufficient stock for product: " + product.getName(), 409);
        }

        product.setStock(newStock);
        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse updatePrice(Long id, BigDecimal price) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        product.setPrice(price);
        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse updateDiscount(Long id, Integer discountPercentage) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));

        product.setDiscountPercentage(discountPercentage);
        return productMapper.toResponse(productRepository.save(product));
    }
}
