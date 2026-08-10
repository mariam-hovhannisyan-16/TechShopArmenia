package am.techshop.order.service;

import am.techshop.common.dto.request.CreateReviewRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(Long userId, CreateReviewRequest request);
    PageResponse<ReviewResponse> getReviewsForProduct(Long productId, int page, int size);
}
