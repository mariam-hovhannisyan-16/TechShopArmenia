package am.techshop.wishlist.service;

import am.techshop.common.dto.response.WishlistResponse;

import java.util.List;

public interface WishlistService {
    WishlistResponse addToWishlist(Long userId, Long productId);
    WishlistResponse getWishlist(Long userId);
    long getWishlistCount(Long userId);
    WishlistResponse removeFromWishlist(Long userId, Long productId);
    List<Long> getSubscriberUserIds(Long productId);
    void deleteWishlistForUser(Long userId);
}
