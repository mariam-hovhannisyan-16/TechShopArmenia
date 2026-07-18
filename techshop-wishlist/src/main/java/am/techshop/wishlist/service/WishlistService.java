package am.techshop.wishlist.service;

import am.techshop.common.dto.response.WishlistResponse;

public interface WishlistService {
    WishlistResponse addToWishlist(Long userId, Long productId);
    WishlistResponse getWishlist(Long userId);
    long getWishlistCount(Long userId);
    WishlistResponse removeFromWishlist(Long userId, Long productId);
}
