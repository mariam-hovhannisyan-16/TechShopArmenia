package am.techshop.wishlist.service.impl;

import am.techshop.common.dto.response.ProductResponse;
import am.techshop.common.dto.response.WishlistItemResponse;
import am.techshop.common.dto.response.WishlistResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.wishlist.client.ProductClient;
import am.techshop.wishlist.entity.Wishlist;
import am.techshop.wishlist.entity.WishlistItem;
import am.techshop.wishlist.mapper.WishlistMapper;
import am.techshop.wishlist.repository.WishlistRepository;
import am.techshop.wishlist.service.WishlistService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistMapper wishlistMapper;
    private final ProductClient productClient;

    public WishlistResponse addToWishlist(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        if (wishlist.hasProduct(productId)) {
            throw new TechShopException("Product already in wishlist", 409);
        }

        fetchProduct(productId);
        wishlist.addItem(wishlistMapper.toItem(productId));

        return buildResponse(wishlistRepository.save(wishlist));
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .map(this::buildResponse)
                .orElseGet(() -> new WishlistResponse(null, userId, List.of(), null));
    }

    @Transactional(readOnly = true)
    public long getWishlistCount(Long userId) {
        return wishlistRepository.countItemsByUserId(userId);
    }

    public WishlistResponse removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = wishlistRepository.findByUserId(userId)
                .orElseThrow(() -> new TechShopException("Wishlist not found", 404));

        if (!wishlist.hasProduct(productId)) {
            throw new TechShopException("Product not found in wishlist", 404);
        }

        wishlist.removeItem(productId);
        return buildResponse(wishlistRepository.save(wishlist));
    }

    @Transactional(readOnly = true)
    public List<Long> getSubscriberUserIds(Long productId) {
        return wishlistRepository.findUserIdsByProductId(productId);
    }

    private Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    try {
                        return wishlistRepository.save(wishlistMapper.toEntity(userId));
                    } catch (DataIntegrityViolationException ex) {
                        throw new TechShopException("Wishlist already exists for this user, please retry", 409);
                    }
                });
    }

    private WishlistResponse buildResponse(Wishlist wishlist) {
        List<Long> productIds = wishlist.getItems().stream()
                .map(WishlistItem::getProductId)
                .distinct()
                .toList();
        Map<Long, ProductResponse> productsById = fetchProductsSafely(productIds);

        List<WishlistItemResponse> items = wishlist.getItems().stream()
                .map(item -> wishlistMapper.toItemResponse(item, productsById.get(item.getProductId())))
                .toList();

        return wishlistMapper.toResponse(wishlist, items);
    }

    private void fetchProduct(Long productId) {
        try {
            var response = productClient.getProduct(productId);
            if (response == null || response.data() == null) {
                throw new TechShopException("Product not found", 404);
            }
        } catch (FeignException.NotFound ex) {
            throw new TechShopException("Product not found", 404);
        } catch (FeignException ex) {
            throw new TechShopException("Product service unavailable", 503);
        }
    }

    private Map<Long, ProductResponse> fetchProductsSafely(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        try {
            var response = productClient.getProducts(productIds);
            List<ProductResponse> products = response != null ? response.data() : null;
            if (products == null) {
                return Map.of();
            }
            return products.stream().collect(Collectors.toMap(ProductResponse::id, product -> product));
        } catch (FeignException ex) {
            return Map.of();
        }
    }
}
