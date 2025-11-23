package com.eris.servicehub.services.payout.implementation;

import com.eris.servicehub.dtos.payout.PayoutResponse;
import com.eris.servicehub.entities.*;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.*;
import com.eris.servicehub.services.payout.PayoutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PayoutServiceImpl implements PayoutService {

    @Autowired private PayoutRepository payoutRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username = (principal instanceof UserDetails) ? ((UserDetails) principal).getUsername() : principal.toString();
        return userRepository.findByEmail(username).orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Override
    @Transactional
    public PayoutResponse processProviderPayout(UUID providerId) {
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found with id: " + providerId));

        List<Order> payableOrders = orderRepository.findPayableOrdersByProviderId(
                providerId, OrderStatus.COMPLETED, PaymentStatus.PAID
        );

        if (payableOrders.isEmpty()) {
            throw new IllegalStateException("No payable orders found for this provider.");
        }

        BigDecimal totalPayoutAmount = payableOrders.stream()
                .map(Order::getNetPayout)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Payout newPayout = Payout.builder()
                .provider(provider)
                .totalAmount(totalPayoutAmount)
                .build();

        List<PayoutItem> payoutItems = payableOrders.stream().map(order -> {
            PayoutItem item = PayoutItem.builder()
                    .payout(newPayout)
                    .order(order)
                    .amount(order.getNetPayout())
                    .build();
            order.setPayoutItem(item);
            return item;
        }).collect(Collectors.toList());

        newPayout.setPayoutItems(payoutItems);
        Payout savedPayout = payoutRepository.save(newPayout);
        return mapToPayoutResponse(savedPayout);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutResponse> getMyPayoutHistory(Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Payout> payoutsPage = payoutRepository.findByProviderIdOrderByPayoutDateDesc(currentUser.getId(), pageable);
        return payoutsPage.map(this::mapToPayoutResponse);
    }

    private PayoutResponse mapToPayoutResponse(Payout payout) {
        List<PayoutResponse.OrderItemSummary> orderSummaries = payout.getPayoutItems().stream()
                .map(item -> PayoutResponse.OrderItemSummary.builder()
                        .orderId(item.getOrder().getId())
                        .netAmount(item.getAmount())
                        .completedDate(item.getOrder().getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return PayoutResponse.builder()
                .id(payout.getId())
                .providerId(payout.getProvider().getId())
                .totalAmount(payout.getTotalAmount())
                .payoutDate(payout.getPayoutDate())
                .numberOfOrders(orderSummaries.size())
                .orders(orderSummaries)
                .build();
    }
}