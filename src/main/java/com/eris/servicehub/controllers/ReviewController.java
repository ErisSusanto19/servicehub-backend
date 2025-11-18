package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.review.ReviewRequest;
import com.eris.servicehub.dtos.review.ReviewResponse;
import com.eris.servicehub.services.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @PostMapping("/order-item/{orderItemId}")
    @PreAuthorize("hasAuthority('CUSTOMER') and @orderSecurity.isCustomerForOrderItem(authentication, #orderItemId)")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable UUID orderItemId,
            @Valid @RequestBody ReviewRequest request
    ) {
        ReviewResponse data = reviewService.createReview(orderItemId, request);
        return new ResponseEntity<>(ApiResponse.success(data, "Review created successfully"), HttpStatus.CREATED);
    }
}