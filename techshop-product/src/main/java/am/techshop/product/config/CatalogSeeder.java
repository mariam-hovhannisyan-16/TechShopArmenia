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

            String proDeepBlueUrl = "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/be/d2/bed2ebac62b52c2580a68b9e0d67d995/250915140029572558.webp";
            Product iphone17Pro = seedProduct("iPhone 17 Pro", "Apple iPhone 17 Pro with A19 Pro chip and titanium design.",
                    new BigDecimal("620000.00"), 18, "Phones", proDeepBlueUrl, true);
            applyIphone17ProVariants(iphone17Pro,
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/8a/8a/8a8ae008f8b519511bfc41e8d16b6f81/250915140023667226.webp",
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/a9/6c/a96c19277c05e15cfeeacee84a370ba0/250915140025661602.webp",
                    proDeepBlueUrl);

            String proMaxDeepBlueUrl = "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/41/49/4149198f1713e718ba920b85acffb4f4/250915140038367093.webp";
            Product iphone17ProMax = seedProduct("iPhone 17 Pro Max",
                    "Apple iPhone 17 Pro Max with A19 Pro chip, titanium design, and the largest display in the lineup.",
                    new BigDecimal("690000.00"), 14, "Phones", proMaxDeepBlueUrl, true);
            applyIphone17ProVariants(iphone17ProMax,
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/2d/22/2d22e5e521d6491cf0dd8c0c8a47f2eb/250915140013863146.webp",
                    "https://prod-cdn.prod.asbis.io/s3size/el:t/f:webp/rt:fill/w:900/plain/s3://cms/product/a3/44/a34423b7b08300fde0625964a0130f66/250915140035204152.webp",
                    proMaxDeepBlueUrl);

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

    private void applyIphone17ProVariants(Product product, String cosmicOrangeImageUrl, String silverImageUrl, String deepBlueImageUrl) {
        product.setStorageOptions(List.of(
                storageOption("256GB", "0.00"),
                storageOption("512GB", "50000.00"),
                storageOption("1TB", "100000.00")));
        product.setSimOptions(List.of("Dual eSIM", "Nano-SIM & eSIM"));
        product.setColorVariants(List.of(
                colorVariant("Cosmic Orange", cosmicOrangeImageUrl),
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
