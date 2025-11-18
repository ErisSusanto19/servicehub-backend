package com.eris.servicehub.services.review;

import com.eris.servicehub.dtos.review.ReviewRequest;
import com.eris.servicehub.dtos.review.ReviewResponse;
import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewResponse createReview(UUID orderItemId, ReviewRequest request);
    List<ReviewResponse> getReviewsForService(UUID serviceId);
}