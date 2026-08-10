package am.techshop.order.service;

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
import am.techshop.order.service.impl.ReviewServiceImpl;
import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 5L;

    private void setInternalApiKey() {
        ReflectionTestUtils.setField(reviewService, "internalApiKey", "test-internal-key");
    }

    @Test
    void createReview_WhenPurchaseVerifiedAndNotAlreadyReviewed_SavesReviewAndSyncsRating() {
        setInternalApiKey();
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great product!");

        when(orderItemRepository.existsVerifiedPurchase(eq(USER_ID), eq(PRODUCT_ID), any())).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });
        when(reviewRepository.averageRatingByProductId(PRODUCT_ID)).thenReturn(new BigDecimal("5"));
        when(reviewRepository.countByProductId(PRODUCT_ID)).thenReturn(1L);

        ReviewResponse response = reviewService.createReview(USER_ID, request);

        assertEquals(10L, response.id());
        assertEquals(PRODUCT_ID, response.productId());
        assertEquals(USER_ID, response.userId());
        assertEquals(5, response.rating());
        assertEquals("Great product!", response.comment());

        ArgumentCaptor<RatingUpdateRequest> captor = ArgumentCaptor.forClass(RatingUpdateRequest.class);
        verify(productClient).updateRating(eq(PRODUCT_ID), captor.capture(), eq("test-internal-key"));
        assertEquals(0, new BigDecimal("5.00").compareTo(captor.getValue().rating()));
        assertEquals(1, captor.getValue().reviewCount());
    }

    @Test
    void createReview_WhenNotPurchased_ThrowsForbiddenAndDoesNotSave() {
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great product!");
        when(orderItemRepository.existsVerifiedPurchase(eq(USER_ID), eq(PRODUCT_ID), any())).thenReturn(false);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> reviewService.createReview(USER_ID, request));

        assertEquals(403, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
        verify(productClient, never()).updateRating(any(), any(), anyString());
    }

    @Test
    void createReview_WhenAlreadyReviewed_ThrowsConflictAndDoesNotSave() {
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great product!");
        when(orderItemRepository.existsVerifiedPurchase(eq(USER_ID), eq(PRODUCT_ID), any())).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(true);

        TechShopException ex = assertThrows(TechShopException.class,
                () -> reviewService.createReview(USER_ID, request));

        assertEquals(409, ex.getStatusCode());
        verify(reviewRepository, never()).save(any());
        verify(productClient, never()).updateRating(any(), any(), anyString());
    }

    @Test
    void createReview_ChecksStatusesIncludeAllVerifiedPurchaseStates() {
        setInternalApiKey();
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 4, "Good.");
        when(orderItemRepository.existsVerifiedPurchase(eq(USER_ID), eq(PRODUCT_ID), any())).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reviewRepository.averageRatingByProductId(PRODUCT_ID)).thenReturn(new BigDecimal("4"));
        when(reviewRepository.countByProductId(PRODUCT_ID)).thenReturn(1L);

        reviewService.createReview(USER_ID, request);

        ArgumentCaptor<List<OrderStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);
        verify(orderItemRepository).existsVerifiedPurchase(eq(USER_ID), eq(PRODUCT_ID), statusesCaptor.capture());
        List<OrderStatus> statuses = statusesCaptor.getValue();
        assertEquals(List.of(OrderStatus.PAID, OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED), statuses);
    }

    @Test
    void createReview_WhenRatingSyncFails_StillSavesReview() {
        setInternalApiKey();
        CreateReviewRequest request = new CreateReviewRequest(PRODUCT_ID, 5, "Great product!");

        when(orderItemRepository.existsVerifiedPurchase(eq(USER_ID), eq(PRODUCT_ID), any())).thenReturn(true);
        when(reviewRepository.existsByProductIdAndUserId(PRODUCT_ID, USER_ID)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });
        when(reviewRepository.averageRatingByProductId(PRODUCT_ID)).thenReturn(new BigDecimal("5"));
        when(reviewRepository.countByProductId(PRODUCT_ID)).thenReturn(1L);
        when(productClient.updateRating(any(), any(), anyString()))
                .thenThrow(mock(FeignException.class));

        ReviewResponse response = reviewService.createReview(USER_ID, request);

        assertEquals(10L, response.id());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void getReviewsForProduct_ReturnsPagedResponse() {
        Review review = new Review();
        review.setId(1L);
        review.setProductId(PRODUCT_ID);
        review.setUserId(USER_ID);
        review.setRating(5);
        review.setComment("Nice");
        Page<Review> page = new PageImpl<>(List.of(review));

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc(eq(PRODUCT_ID), any(Pageable.class))).thenReturn(page);

        PageResponse<ReviewResponse> result = reviewService.getReviewsForProduct(PRODUCT_ID, 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(1, result.totalElements());
        assertEquals(5, result.content().get(0).rating());
    }
}
