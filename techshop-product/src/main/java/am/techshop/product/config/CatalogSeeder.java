package am.techshop.product.config;

import am.techshop.product.entity.Category;
import am.techshop.product.entity.Product;
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

    @Override
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            CATEGORIES.forEach(name -> categoryRepository.save(Category.builder().name(name).build()));
        }

        if (productRepository.count() == 0) {
            productRepository.saveAll(List.of(
                    Product.builder()
                            .name("iPhone 15, 128GB")
                            .description("Apple iPhone 15 with 128GB storage.")
                            .price(new BigDecimal("450000.00"))
                            .stock(25)
                            .category("Phones")
                            .imageUrl("/images/products/iphone-15.jpg")
                            .isNew(true)
                            .build(),
                    Product.builder()
                            .name("Samsung Galaxy S24")
                            .description("Samsung Galaxy S24 with 256GB storage.")
                            .price(new BigDecimal("420000.00"))
                            .stock(30)
                            .category("Phones")
                            .imageUrl("/images/products/galaxy-s24.jpg")
                            .isNew(true)
                            .build(),
                    Product.builder()
                            .name("MacBook Air M2")
                            .description("Apple MacBook Air with M2 chip, 13-inch.")
                            .price(new BigDecimal("650000.00"))
                            .stock(15)
                            .category("Laptops")
                            .imageUrl("/images/products/macbook-air-m2.jpg")
                            .isNew(false)
                            .build(),
                    Product.builder()
                            .name("LG 55\" 4K Smart TV")
                            .description("LG 55-inch 4K UHD Smart TV.")
                            .price(new BigDecimal("380000.00"))
                            .stock(10)
                            .category("TVs")
                            .imageUrl("/images/products/lg-55-4k.jpg")
                            .isNew(false)
                            .build(),
                    Product.builder()
                            .name("Sony WH-1000XM5")
                            .description("Sony WH-1000XM5 noise-cancelling headphones.")
                            .price(new BigDecimal("165000.00"))
                            .stock(40)
                            .category("Audio")
                            .imageUrl("/images/products/sony-wh1000xm5.jpg")
                            .isNew(true)
                            .build(),
                    Product.builder()
                            .name("Canon EOS R50")
                            .description("Canon EOS R50 mirrorless camera with kit lens.")
                            .price(new BigDecimal("520000.00"))
                            .stock(8)
                            .category("Cameras")
                            .imageUrl("/images/products/canon-eos-r50.jpg")
                            .isNew(false)
                            .build()
            ));
        }
    }
}
