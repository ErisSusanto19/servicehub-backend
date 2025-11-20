package com.eris.servicehub.services.review.implementation;

import com.eris.servicehub.dtos.review.ReviewRequest;
import com.eris.servicehub.dtos.review.ReviewResponse;
import com.eris.servicehub.entities.*;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.*;
import com.eris.servicehub.services.notification.NotificationService;
import com.eris.servicehub.services.review.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private OrderItemRepository orderItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationService notificationService;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + username));
    }

    @Override
    @Transactional
    public ReviewResponse createReview(UUID orderItemId, ReviewRequest request) {
        User customer = getCurrentUser();
        OrderItem orderItem = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + orderItemId));

        if (!orderItem.getOrder().getCustomer().getId().equals(customer.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the customer for this order item.");
        }

        if (orderItem.getOrder().getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("You can only review completed orders.");
        }

        if (reviewRepository.existsByOrderItemId(orderItemId)) {
            throw new IllegalStateException("A review has already been submitted for this order item.");
        }

        Review review = Review.builder()
                .rating(request.rating())
                .comment(request.comment())
                .customer(customer)
                .service(orderItem.getService())
                .orderItem(orderItem)
                .build();

        Review savedReview = reviewRepository.save(review);

        User provider = savedReview.getService().getProvider();
        String message = String.format("Your '%s' service received a new review from %s.", savedReview.getService().getName(), savedReview.getCustomer().getName());
        String link = "/services/" + savedReview.getService().getId() + "/reviews";
        notificationService.createNotification(provider, message, link);

        return mapToResponse(savedReview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsForService(UUID serviceId) {
        return reviewRepository.findByServiceId(serviceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse mapToResponse(Review review) {
        ReviewResponse.CustomerSummary customerSummary = ReviewResponse.CustomerSummary.builder()
                .id(review.getCustomer().getId())
                .name(review.getCustomer().getName())
                .build();

        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .customer(customerSummary)
                .createdAt(review.getCreatedAt())
                .build();
    }
}