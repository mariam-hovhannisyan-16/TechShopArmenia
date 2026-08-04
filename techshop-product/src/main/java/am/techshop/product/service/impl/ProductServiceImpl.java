package am.techshop.product.service.impl;

import am.techshop.common.dto.request.ProductRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.PricePredictionResponse;
import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.SurpriseBoxResponse;
import am.techshop.common.event.PriceDropEvent;
import am.techshop.common.exception.ProductNotFoundException;
import am.techshop.common.exception.TechShopException;
import am.techshop.product.client.WishlistClient;
import am.techshop.product.entity.PriceHistory;
import am.techshop.product.entity.Product;
import am.techshop.product.kafka.ProductEventProducer;
import am.techshop.product.mapper.ProductMapper;
import am.techshop.product.repository.CategoryRepository;
import am.techshop.product.repository.PriceHistoryRepository;
import am.techshop.product.repository.ProductRepository;
import am.techshop.product.repository.ProductSpecifications;
import am.techshop.product.service.PricePredictionService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final WishlistClient wishlistClient;
    private final ProductEventProducer productEventProducer;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PricePredictionService pricePredictionService;

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

    public ProductResponse updatePrice(Long id, BigDecimal price) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        BigDecimal oldPrice = product.getPrice();
        priceHistoryRepository.save(new PriceHistory(null, id, oldPrice, price, LocalDateTime.now()));

        product.setPrice(price);
        ProductResponse response = productMapper.toResponse(productRepository.save(product));

        if (oldPrice != null && price.compareTo(oldPrice) < 0) {
            notifyPriceDropSubscribers(product, price);
        }

        return response;
    }

    private void notifyPriceDropSubscribers(Product product, BigDecimal newPrice) {
        try {
            List<Long> subscriberIds = wishlistClient.getSubscribers(product.getId(), internalApiKey).data();
            if (subscriberIds == null) {
                return;
            }
            subscriberIds.forEach(userId -> productEventProducer.sendPriceDropEvent(
                    new PriceDropEvent(userId, product.getId(), product.getName(), newPrice)));
        } catch (Exception ex) {
            log.warn("Failed to notify wishlist subscribers of price drop for product {}: {}", product.getId(), ex.getMessage());
        }
    }

    public ProductResponse updateDiscount(Long id, Integer discountPercentage) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.setDiscountPercentage(discountPercentage);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public PricePredictionResponse getPricePrediction(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        List<PriceHistory> history = priceHistoryRepository.findByProductIdAndChangedAtAfterOrderByChangedAtAsc(
                id, LocalDateTime.now().minusDays(90));
        if (history.size() < PricePredictionService.MIN_HISTORY_RECORDS) {
            // The 90-day window came up too sparse to judge a trend from - widen to the
            // product's full price history instead of giving up immediately.
            history = priceHistoryRepository.findByProductIdOrderByChangedAtAsc(id);
        }

        return pricePredictionService.getPrediction(id, history);
    }

    @Transactional(readOnly = true)
    public SurpriseBoxResponse getSurpriseBox(BigDecimal budget) {
        List<Product> available = new ArrayList<>(productRepository.findByStockGreaterThan(0));
        Collections.shuffle(available);

        List<Product> selected = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();
        Set<String> usedCategories = new HashSet<>();
        BigDecimal remaining = budget;

        // First pass: one item per category, for variety, while budget allows.
        for (Product product : available) {
            if (!usedCategories.contains(product.getCategory()) && product.getPrice().compareTo(remaining) <= 0) {
                selected.add(product);
                selectedIds.add(product.getId());
                usedCategories.add(product.getCategory());
                remaining = remaining.subtract(product.getPrice());
            }
        }

        // Second pass: fill whatever budget is left with anything else that still fits.
        for (Product product : available) {
            if (selectedIds.contains(product.getId())) {
                continue;
            }
            if (product.getPrice().compareTo(remaining) <= 0) {
                selected.add(product);
                selectedIds.add(product.getId());
                remaining = remaining.subtract(product.getPrice());
            }
        }

        BigDecimal totalPrice = budget.subtract(remaining);
        return new SurpriseBoxResponse(
                selected.stream().map(productMapper::toResponse).toList(),
                totalPrice,
                remaining
        );
    }
}
