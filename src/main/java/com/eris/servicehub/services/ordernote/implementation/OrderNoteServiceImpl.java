package com.eris.servicehub.services.ordernote.implementation;

import com.eris.servicehub.dtos.ordernote.OrderNoteRequest;
import com.eris.servicehub.dtos.ordernote.OrderNoteResponse;
import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.OrderNote;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.OrderNoteRepository;
import com.eris.servicehub.repositories.OrderRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.security.OrderSecurity;
import com.eris.servicehub.services.ordernote.OrderNoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderNoteServiceImpl implements OrderNoteService {

    @Autowired private OrderNoteRepository orderNoteRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderSecurity orderSecurity;

    private User getCurrentUser() {
        String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return userRepository.findByEmail(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void checkOrderParticipation(User user, Order order) {
        boolean isCustomer = order.getCustomer().getId().equals(user.getId());
        boolean isProvider = order.getOrderItems().stream()
                .anyMatch(item -> item.getService().getProvider().getId().equals(user.getId()));
        if (!isCustomer && !isProvider) {
            throw new AccessDeniedException("You are not a participant in this order.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderNoteResponse> getNotesForOrder(UUID orderId) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        checkOrderParticipation(currentUser, order);

        return orderNoteRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderNoteResponse createNoteForOrder(UUID orderId, OrderNoteRequest request) {
        User currentUser = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        checkOrderParticipation(currentUser, order);

        OrderNote newNote = OrderNote.builder()
                .order(order)
                .author(currentUser)
                .content(request.content())
                .build();

        OrderNote savedNote = orderNoteRepository.save(newNote);
        return mapToResponse(savedNote);
    }

    private OrderNoteResponse mapToResponse(OrderNote note) {
        OrderNoteResponse.AuthorSummary authorSummary = OrderNoteResponse.AuthorSummary.builder()
                .id(note.getAuthor().getId())
                .name(note.getAuthor().getName())
                .build();

        return OrderNoteResponse.builder()
                .id(note.getId())
                .content(note.getContent())
                .author(authorSummary)
                .createdAt(note.getCreatedAt())
                .build();
    }
}