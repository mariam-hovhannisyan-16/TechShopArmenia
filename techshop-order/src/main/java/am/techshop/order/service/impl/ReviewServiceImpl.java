package am.techshop.order.service.impl;

import am.techshop.common.dto.request.CreateReviewRequest;
import am.techshop.common.dto.request.RatingUpdateRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ReviewResponse;
import am.techshop.common.enums.OrderStatus;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.client.ProductClient;
import am.techshop.order.entity.Review;
import am.techshop.order.repository.OrderItemRepository;
import am.techshop.order.repository.ReviewRepository;
import am.techshop.order.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private static final List<OrderStatus> VERIFIED_PURCHASE_STATUSES =
            List.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductClient productClient;

    @Value("${internal.api-key}")
    private String internalApiKey;

    public ReviewResponse createReview(Long userId, CreateReviewRequest request) {
        if (!orderItemRepository.existsVerifiedPurchase(userId, request.productId(), VERIFIED_PURCHASE_STATUSES)) {
            throw new TechShopException("You can only review products you have purchased", 403);
        }

        if (reviewRepository.existsByProductIdAndUserId(request.productId(), userId)) {
            throw new TechShopException("You have already reviewed this product", 409);
        }

        Review review = new Review();
        review.setProductId(request.productId());
        review.setUserId(userId);
        review.setRating(request.rating());
        review.setComment(request.comment());
        Review saved = reviewRepository.save(review);

        syncProductRating(request.productId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getReviewsForProduct(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Review> result = reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);

        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private void syncProductRating(Long productId) {
        try {
            BigDecimal average = reviewRepository.averageRatingByProductId(productId)
                    .setScale(2, RoundingMode.HALF_UP);
            long count = reviewRepository.countByProductId(productId);
            productClient.updateRating(productId, new RatingUpdateRequest(average, (int) count), internalApiKey);
        } catch (Exception ex) {
            log.warn("Failed to sync rating for product {}: {}", productId, ex.getMessage());
        }
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(), review.getProductId(), review.getUserId(),
                review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
