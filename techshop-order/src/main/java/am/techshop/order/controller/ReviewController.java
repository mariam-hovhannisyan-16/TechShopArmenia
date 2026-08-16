package am.techshop.order.controller;

import am.techshop.common.dto.request.CreateReviewRequest;
import am.techshop.common.dto.response.ApiResponse;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ReviewResponse;
import am.techshop.common.security.CurrentUser;
import am.techshop.order.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reviews", description = "Submit and browse product reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit a review for a product")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @RequestBody @Valid CreateReviewRequest request, Authentication authentication) {
        ReviewResponse response = reviewService.createReview(CurrentUser.id(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Review submitted", response));
    }

    @GetMapping
    @Operation(summary = "List reviews for a product")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getReviews(
            @RequestParam @NotNull Long productId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviewsForProduct(productId, page, size)));
    }
}
