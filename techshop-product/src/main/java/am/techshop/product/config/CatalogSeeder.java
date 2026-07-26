package am.techshop.product.config;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.product.entity.Product;
import am.techshop.product.mapper.CategoryMapper;
import am.techshop.product.mapper.ProductMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogSeeder implements ApplicationRunner {

    private static final List<String> CATEGORIES = List.of("Phones", "Laptops", "TVs", "Audio", "Cameras");

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            CATEGORIES.forEach(name -> categoryRepository.save(categoryMapper.toEntity(name)));
        }

        if (productRepository.count() == 0) {
            productRepository.saveAll(List.of(
                    seedProduct("iPhone 15, 128GB", "Apple iPhone 15 with 128GB storage.",
                            new BigDecimal("450000.00"), 25, "Phones", "/images/products/iphone-15.jpg", true),
                    seedProduct("Samsung Galaxy S24", "Samsung Galaxy S24 with 256GB storage.",
                            new BigDecimal("420000.00"), 30, "Phones", "/images/products/galaxy-s24.jpg", true),
                    seedProduct("MacBook Air M2", "Apple MacBook Air with M2 chip, 13-inch.",
                            new BigDecimal("650000.00"), 15, "Laptops", "/images/products/macbook-air-m2.jpg", false),
                    seedProduct("LG 55\" 4K Smart TV", "LG 55-inch 4K UHD Smart TV.",
                            new BigDecimal("380000.00"), 10, "TVs", "/images/products/lg-55-4k.jpg", false),
                    seedProduct("Sony WH-1000XM5", "Sony WH-1000XM5 noise-cancelling headphones.",
                            new BigDecimal("165000.00"), 40, "Audio", "/images/products/sony-wh1000xm5.jpg", true),
                    seedProduct("Canon EOS R50", "Canon EOS R50 mirrorless camera with kit lens.",
                            new BigDecimal("520000.00"), 8, "Cameras", "/images/products/canon-eos-r50.jpg", false)
            ));
        }
    }

    private Product seedProduct(String name, String description, BigDecimal price, int stock,
                                 String category, String imageUrl, boolean isNew) {
        return productMapper.toEntity(new ProductRequest(name, description, price, stock, category, imageUrl, isNew));
    }
}
