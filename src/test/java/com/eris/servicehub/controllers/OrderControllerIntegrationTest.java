package com.eris.servicehub.controllers;

import com.eris.servicehub.AbstractIntegrationTest;
import com.eris.servicehub.config.TestConfig;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;
import com.eris.servicehub.entities.*;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import com.eris.servicehub.repositories.*;
import com.eris.servicehub.services.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestConfig.class)
public class OrderControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private NotificationRepository notificationRepository;

    private User customer;
    private User provider;
    private Service serviceToOrder;
    private Order existingOrder;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        serviceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role customerRole = roleRepository.save(Role.builder().name("CUSTOMER").build());
        Role providerRole = roleRepository.save(Role.builder().name("PROVIDER").build());

        customer = User.builder()
                .name("Test Customer")
                .email("customer@test.com")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of(customerRole))
                .build();
        userRepository.save(customer);

        provider = User.builder()
                .name("Test Provider")
                .email("provider@test.com")
                .password(passwordEncoder.encode("password"))
                .roles(Set.of(providerRole))
                .build();
        userRepository.save(provider);

        Category category = categoryRepository.save(Category.builder().name("Test Category").build());
        serviceToOrder = Service.builder()
                .name("Test Service")
                .price(new BigDecimal("100.00"))
                .provider(provider)
                .category(category)
                .build();
        serviceRepository.save(serviceToOrder);

        existingOrder = com.eris.servicehub.entities.Order.builder()
                .customer(customer)
                .totalPrice(new BigDecimal("100.00"))
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PAID)
                .platformFee(BigDecimal.ZERO)
                .netPayout(BigDecimal.ZERO)
                .orderItems(List.of(
                        OrderItem.builder().service(serviceToOrder).priceAtOrder(serviceToOrder.getPrice()).build()
                ))
                .build();

        existingOrder.getOrderItems().forEach(item -> item.setOrder(existingOrder));
        orderRepository.save(existingOrder);
    }

    private String generateJwtToken(String email) {
        return jwtService.generateToken(userDetailsService.loadUserByUsername(email));
    }

    @Test
    void createOrder_whenUserIsCustomerAndServiceExists_shouldSucceed() {
        // --- ARRANGE ---
        String token = generateJwtToken(customer.getEmail());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        OrderRequest orderRequest = new OrderRequest(List.of(serviceToOrder.getId()));
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse<OrderResponse>> response = restTemplate.exchange(
                "/api/orders",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID newOrderId = response.getBody().getData().getId();

        com.eris.servicehub.entities.Order savedOrder = orderRepository.findById(newOrderId)
                .orElseThrow(() -> new AssertionError("Order not found in DB"));

        assertThat(savedOrder.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(savedOrder.getTotalPrice()).isEqualByComparingTo(serviceToOrder.getPrice());

        assertThat(savedOrder.getOrderItems()).hasSize(1);
        assertThat(savedOrder.getOrderItems().get(0).getService().getId()).isEqualTo(serviceToOrder.getId());
    }

    @Test
    void createOrder_whenUserIsNotAuthenticated_shouldFailWithForbidden() {
        // --- ARRANGE ---
        OrderRequest orderRequest = new OrderRequest(List.of(serviceToOrder.getId()));
        HttpEntity<OrderRequest> requestEntity = new HttpEntity<>(orderRequest);

        // --- ACT ---
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/orders",
                HttpMethod.POST,
                requestEntity,
                ApiResponse.class
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(orderRepository.count()).isEqualTo(1L);
    }

    @Test
    void updateOrderStatus_whenUserIsProviderForOrder_shouldSucceed() {
        // --- ARRANGE ---
        String providerToken = generateJwtToken(provider.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(providerToken);

        UpdateOrderStatusRequest requestBody = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);
        HttpEntity<UpdateOrderStatusRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse<OrderResponse>> response = restTemplate.exchange(
                "/api/orders/" + existingOrder.getId() + "/status",
                HttpMethod.PATCH,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getStatus()).isEqualTo(OrderStatus.ACCEPTED);

        com.eris.servicehub.entities.Order updatedOrder = orderRepository.findById(existingOrder.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
    }
}