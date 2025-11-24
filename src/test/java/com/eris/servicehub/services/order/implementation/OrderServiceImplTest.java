package com.eris.servicehub.services.order.implementation;

import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;
import com.eris.servicehub.entities.Category;
import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.Service;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.repositories.OrderRepository;
import com.eris.servicehub.repositories.ServiceRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.notification.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Test
    void createOrder_shouldCalculateTotalPriceCorrectlyAndSaveOrder() {
        // --- ARRANGE ---
        UUID serviceId1 = UUID.randomUUID();
        UUID serviceId2 = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).name("Dummy Customer").build();
        Service service1 = Service.builder().id(serviceId1).price(new BigDecimal("150.50")).provider(new User()).build();
        Service service2 = Service.builder().id(serviceId2).price(new BigDecimal("49.50")).provider(new User()).build();
        OrderRequest request = new OrderRequest(List.of(serviceId1, serviceId2));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("customer@test.com");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByEmail("customer@test.com")).thenReturn(Optional.of(customer));
        when(serviceRepository.findAllById(anyList())).thenReturn(List.of(service1, service2));

        when(orderRepository.save(any(com.eris.servicehub.entities.Order.class)))
                .thenAnswer(invocation -> {
                    com.eris.servicehub.entities.Order orderToSave = invocation.getArgument(0);
                    orderToSave.setId(UUID.randomUUID());
                    return orderToSave;
                });


        // --- ACT ---
        OrderResponse response = orderServiceImpl.createOrder(request);

        // --- ASSERT ---
        // 150.50 + 49.50 = 200.00
        assertThat(response.getTotalPrice()).isEqualByComparingTo("200.00");

        ArgumentCaptor<com.eris.servicehub.entities.Order> orderCaptor = ArgumentCaptor.forClass(com.eris.servicehub.entities.Order.class);

        verify(orderRepository, times(1)).save(orderCaptor.capture());

        com.eris.servicehub.entities.Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getCustomer()).isEqualTo(customer);
        assertThat(capturedOrder.getTotalPrice()).isEqualByComparingTo("200.00");
        assertThat(capturedOrder.getOrderItems()).hasSize(2);
    }

    @Test
    @DisplayName("updateOrderStatus should succeed for valid transition (ACCEPTED to IN_PROGRESS)")
    void updateOrderStatus_withValidTransition_shouldUpdateStatus() {
        // --- ARRANGE ---
        UUID orderId = UUID.randomUUID();
        User customer = User.builder().id(UUID.randomUUID()).build();
        Order existingOrder = Order.builder()
                .id(orderId)
                .status(OrderStatus.ACCEPTED)
                .customer(customer)
                .build();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.IN_PROGRESS);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- ACT ---
        orderServiceImpl.updateOrderStatus(orderId, request);

        // --- ASSERT ---
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        verify(orderRepository, times(1)).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("updateOrderStatus should throw exception for invalid transition (PENDING to COMPLETED)")
    void updateOrderStatus_withInvalidTransition_shouldThrowException() {
        // --- ARRANGE ---
        UUID orderId = UUID.randomUUID();
        Order existingOrder = Order.builder()
                .id(orderId)
                .status(OrderStatus.PENDING)
                .build();

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.COMPLETED); // Status tujuan yang tidak valid

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));

        // --- ACT & ASSERT ---
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderServiceImpl.updateOrderStatus(orderId, request);
        });

        assertThat(exception.getMessage()).contains("From PENDING, provider can only move to ACCEPTED or REJECTED.");

        verify(orderRepository, never()).save(any(Order.class));
    }
}