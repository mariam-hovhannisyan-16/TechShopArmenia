package am.techshop.product.config;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.product.entity.ColorVariant;
import am.techshop.product.entity.Product;
import am.techshop.product.entity.StorageOption;
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
            Product iphone15 = seedProduct("iPhone 15, 128GB", "Apple iPhone 15 with 128GB storage.",
                    new BigDecimal("450000.00"), 25, "Phones",
                    "https://images.unsplash.com/photo-1736173155811-e8142fd553ee?w=900&q=82&fit=crop&auto=format", true);
            iphone15.setStorageOptions(List.of(
                    storageOption("128GB", "0.00"),
                    storageOption("256GB", "40000.00"),
                    storageOption("512GB", "80000.00")));

            Product iphone17Pro = seedProduct("iPhone 17 Pro", "Apple iPhone 17 Pro with A19 Pro chip and titanium design.",
                    new BigDecimal("620000.00"), 18, "Phones",
                    "https://images.unsplash.com/photo-1759588071781-2c3ba9128497?w=900&q=82&fit=crop&auto=format", true);
            applyIphone17ProVariants(iphone17Pro,
                    "https://images.unsplash.com/photo-1609692814858-f7cd2f0afa4f?w=900&q=82&fit=crop&auto=format",
                    "https://images.unsplash.com/photo-1759588071781-2c3ba9128497?w=900&q=82&fit=crop&auto=format",
                    "https://images.unsplash.com/photo-1711967299865-c88350fddb70?w=900&q=82&fit=crop&auto=format");

            Product iphone17ProMax = seedProduct("iPhone 17 Pro Max",
                    "Apple iPhone 17 Pro Max with A19 Pro chip, titanium design, and the largest display in the lineup.",
                    new BigDecimal("690000.00"), 14, "Phones",
                    "https://images.unsplash.com/photo-1759588071838-d560be56b2a2?w=900&q=82&fit=crop&auto=format", true);
            applyIphone17ProVariants(iphone17ProMax,
                    "https://images.unsplash.com/photo-1596558450268-9c27524ba856?w=900&q=82&fit=crop&auto=format",
                    "https://images.unsplash.com/photo-1694570149728-b1011c2a772b?w=900&q=82&fit=crop&auto=format",
                    "https://images.unsplash.com/photo-1604194868790-e98f6e9c5ed4?w=900&q=82&fit=crop&auto=format");

            productRepository.saveAll(List.of(
                    iphone15,
                    seedProduct("Samsung Galaxy S24", "Samsung Galaxy S24 with 256GB storage.",
                            new BigDecimal("420000.00"), 30, "Phones",
                            "https://images.unsplash.com/photo-1706372124814-417e2f0c3fe0?w=900&q=82&fit=crop&auto=format", true),
                    iphone17Pro,
                    iphone17ProMax,
                    seedProduct("MacBook Air M2", "Apple MacBook Air with M2 chip, 13-inch.",
                            new BigDecimal("650000.00"), 15, "Laptops",
                            "https://images.unsplash.com/photo-1651241680016-cc9e407e7dc3?w=900&q=82&fit=crop&auto=format", false),
                    seedProduct("LG 55\" 4K Smart TV", "LG 55-inch 4K UHD Smart TV.",
                            new BigDecimal("380000.00"), 10, "TVs",
                            "https://images.unsplash.com/photo-1689686998931-858488b0c62c?w=900&q=82&fit=crop&auto=format", false),
                    seedProduct("Sony WH-1000XM5", "Sony WH-1000XM5 noise-cancelling headphones.",
                            new BigDecimal("165000.00"), 40, "Audio",
                            "https://images.unsplash.com/photo-1612858249816-5a91a9fb9886?w=900&q=82&fit=crop&auto=format", true),
                    seedProduct("Canon EOS R50", "Canon EOS R50 mirrorless camera with kit lens.",
                            new BigDecimal("520000.00"), 8, "Cameras",
                            "https://images.unsplash.com/photo-1500634245200-e5245c7574ef?w=900&q=82&fit=crop&auto=format", false)
            ));
        }
    }

    private Product seedProduct(String name, String description, BigDecimal price, int stock,
                                 String category, String imageUrl, boolean isNew) {
        return productMapper.toEntity(new ProductRequest(name, description, price, stock, category, imageUrl, isNew));
    }

    private void applyIphone17ProVariants(Product product, String spaceGrayImageUrl, String silverImageUrl, String deepBlueImageUrl) {
        product.setStorageOptions(List.of(
                storageOption("256GB", "0.00"),
                storageOption("512GB", "50000.00"),
                storageOption("1TB", "100000.00")));
        product.setSimOptions(List.of("Dual eSIM", "Nano-SIM & eSIM"));
        product.setColorVariants(List.of(
                colorVariant("Space Gray", spaceGrayImageUrl),
                colorVariant("Silver", silverImageUrl),
                colorVariant("Deep Blue", deepBlueImageUrl)));
    }

    private StorageOption storageOption(String label, String priceDelta) {
        return new StorageOption(label, new BigDecimal(priceDelta));
    }

    private ColorVariant colorVariant(String label, String imageUrl) {
        return new ColorVariant(label, imageUrl);
    }
}
