package com.eris.servicehub.services.review;

import com.eris.servicehub.dtos.review.ReviewRequest;
import com.eris.servicehub.dtos.review.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(UUID orderItemId, ReviewRequest request);
    Page<ReviewResponse> getReviewsForService(UUID serviceId, Pageable pageable);
}