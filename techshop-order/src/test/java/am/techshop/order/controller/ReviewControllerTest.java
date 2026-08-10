package am.techshop.order.controller;

import am.techshop.common.dto.request.CreateReviewRequest;
import am.techshop.common.dto.response.PageResponse;
import am.techshop.common.dto.response.ReviewResponse;
import am.techshop.common.exception.TechShopException;
import am.techshop.order.config.SecurityConfig;
import am.techshop.order.security.JwtAuthFilter;
import am.techshop.order.security.JwtService;
import am.techshop.order.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class})
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @SuppressWarnings("unused")
    @MockBean
    private JwtService jwtService;

    private static final Long USER_ID = 1L;

    private Authentication asUser() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private Authentication asAdmin() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private static ReviewResponse sampleReview() {
        return new ReviewResponse(1L, 5L, USER_ID, 5, "Great product!", LocalDateTime.now());
    }

    @Test
    void createReview_WhenCalledByCustomer_ReturnsCreatedReview() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(5L, 5, "Great product!");
        when(reviewService.createReview(eq(USER_ID), any(CreateReviewRequest.class))).thenReturn(sampleReview());

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Review submitted"))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    void createReview_WhenNotPurchased_ReturnsForbidden() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(5L, 5, "Great product!");
        doThrow(new TechShopException("You can only review products you have purchased", 403))
                .when(reviewService).createReview(eq(USER_ID), any(CreateReviewRequest.class));

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createReview_WhenAlreadyReviewed_ReturnsConflict() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(5L, 5, "Great product!");
        doThrow(new TechShopException("You have already reviewed this product", 409))
                .when(reviewService).createReview(eq(USER_ID), any(CreateReviewRequest.class));

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void createReview_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(5L, 5, "Great product!");

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_WhenCalledByAdmin_ReturnsForbidden() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(5L, 5, "Great product!");

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(asAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_WithRatingOutOfRange_ReturnsBadRequest() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(5L, 6, "Great product!");

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void createReview_WithBlankComment_ReturnsBadRequest() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest(5L, 5, "  ");

        mockMvc.perform(post("/api/reviews")
                        .with(authentication(asUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), any());
    }

    @Test
    void getReviews_ReturnsPagedReviews() throws Exception {
        PageResponse<ReviewResponse> page = new PageResponse<>(List.of(sampleReview()), 0, 20, 1, 1);
        when(reviewService.getReviewsForProduct(5L, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/reviews").param("productId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getReviews_WithoutAuthentication_IsAllowed() throws Exception {
        PageResponse<ReviewResponse> page = new PageResponse<>(List.of(), 0, 20, 0, 0);
        when(reviewService.getReviewsForProduct(5L, 0, 20)).thenReturn(page);

        mockMvc.perform(get("/api/reviews").param("productId", "5"))
                .andExpect(status().isOk());
    }
}
