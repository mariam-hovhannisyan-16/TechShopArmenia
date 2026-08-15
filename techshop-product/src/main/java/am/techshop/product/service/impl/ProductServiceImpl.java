package am.techshop.product.service.impl;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.UserLanguageResponse;
import am.techshop.common.enums.Language;
import am.techshop.common.event.PriceDropEvent;
import am.techshop.common.exception.ProductNotFoundException;
import am.techshop.common.exception.TechShopException;
import am.techshop.product.client.UserClient;
import am.techshop.product.client.WishlistClient;
import am.techshop.product.entity.PriceHistory;
import am.techshop.product.entity.Product;
import am.techshop.product.kafka.ProductEventProducer;
import am.techshop.product.mapper.ProductMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.repository.PriceHistoryRepository;
import am.techshop.product.repository.ProductRepository;
import am.techshop.product.repository.ProductSpecifications;
import am.techshop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final WishlistClient wishlistClient;
    private final UserClient userClient;
    private final ProductEventProducer productEventProducer;
    private final PriceHistoryRepository priceHistoryRepository;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public ProductResponse addProduct(ProductRequest request) {
        if (!categoryRepository.existsByNameIgnoreCase(request.category())) {
            throw new TechShopException("Unknown category: %s".formatted(request.category()), 400);
        }

        Product product = productMapper.toEntity(request);
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
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        return productRepository.findAllById(ids).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    public ProductResponse adjustStock(Long id, int quantityDelta) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        int newStock = product.getStock() + quantityDelta;
        if (newStock < 0) {
            throw new TechShopException("Insufficient stock for product: %s".formatted(product.getName()), 409);
        }

        product.setStock(newStock);
        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse updateStock(Long id, int quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStock(quantity);
        return productMapper.toResponse(productRepository.save(product));
    }

    public ProductResponse updatePrice(Long id, BigDecimal price) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        BigDecimal oldPrice = product.getPrice();
        priceHistoryRepository.save(new PriceHistory(null, id, oldPrice, price, LocalDateTime.now()));

        BigDecimal oldEffectivePrice = oldPrice == null ? null : effectivePrice(oldPrice, product.getDiscountPercentage());

        product.setPrice(price);
        ProductResponse response = productMapper.toResponse(productRepository.save(product));

        if (oldEffectivePrice != null) {
            BigDecimal newEffectivePrice = effectivePrice(price, product.getDiscountPercentage());
            if (newEffectivePrice.compareTo(oldEffectivePrice) < 0) {
                notifyPriceDropSubscribers(product, oldEffectivePrice, newEffectivePrice);
            }
        }

        return response;
    }

    public ProductResponse updateDiscount(Long id, Integer discountPercentage) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        BigDecimal oldEffectivePrice = effectivePrice(product.getPrice(), product.getDiscountPercentage());

        product.setDiscountPercentage(discountPercentage);
        ProductResponse response = productMapper.toResponse(productRepository.save(product));

        if (oldEffectivePrice != null) {
            BigDecimal newEffectivePrice = effectivePrice(product.getPrice(), discountPercentage);
            if (newEffectivePrice.compareTo(oldEffectivePrice) < 0) {
                notifyPriceDropSubscribers(product, oldEffectivePrice, newEffectivePrice);
            }
        }

        return response;
    }

    private static BigDecimal effectivePrice(BigDecimal price, Integer discountPercentage) {
        if (price == null) {
            return null;
        }
        if (discountPercentage == null || discountPercentage == 0) {
            return price;
        }
        return price.multiply(BigDecimal.valueOf(100 - discountPercentage)).divide(BigDecimal.valueOf(100));
    }

    private void notifyPriceDropSubscribers(Product product, BigDecimal oldPrice, BigDecimal newPrice) {
        try {
            List<Long> subscriberIds = wishlistClient.getSubscribers(product.getId(), internalApiKey).data();
            if (subscriberIds == null || subscriberIds.isEmpty()) {
                return;
            }
            List<Long> optedInIds = userClient.getPriceDropEnabledUserIds(subscriberIds, internalApiKey).data();
            if (optedInIds == null || optedInIds.isEmpty()) {
                return;
            }
            Map<Long, Language> languagesByUserId = userClient.getUserLanguages(optedInIds, internalApiKey).data().stream()
                    .collect(Collectors.toMap(UserLanguageResponse::userId, UserLanguageResponse::language));
            optedInIds.forEach(userId -> productEventProducer.sendPriceDropEvent(
                    new PriceDropEvent(userId, product.getId(), product.getName(), oldPrice, newPrice,
                            languagesByUserId.getOrDefault(userId, Language.HY))));
        } catch (Exception ex) {
            log.warn("Failed to notify wishlist subscribers of price drop for product {}: {}", product.getId(), ex.getMessage());
        }
    }

    public ProductResponse updateRating(Long id, BigDecimal rating, Integer reviewCount) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setRating(rating);
        product.setReviewCount(reviewCount);
        return productMapper.toResponse(productRepository.save(product));
    }
}
