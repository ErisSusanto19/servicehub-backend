package com.eris.servicehub.controllers;

import com.eris.servicehub.AbstractIntegrationTest;
import com.eris.servicehub.config.TestConfig;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.review.ReviewRequest;
import com.eris.servicehub.dtos.review.ReviewResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestConfig.class)
public class ReviewControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private NotificationRepository notificationRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;

    private User customer;
    private User provider;
    private OrderItem completedOrderItem;
    private OrderItem pendingOrderItem;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        serviceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role customerRole = roleRepository.save(Role.builder().name("CUSTOMER").build());
        Role providerRole = roleRepository.save(Role.builder().name("PROVIDER").build());

        customer = User.builder().name("Test Customer").email("customer@test.com").password(passwordEncoder.encode("password")).roles(Set.of(customerRole)).build();
        provider = User.builder().name("Test Provider").email("provider@test.com").password(passwordEncoder.encode("password")).roles(Set.of(providerRole)).build();
        userRepository.saveAll(List.of(customer, provider));

        Category category = categoryRepository.save(Category.builder().name("Test Category").build());
        Service service = serviceRepository.save(Service.builder().name("Test Service").price(new BigDecimal("100.00")).provider(provider).category(category).build());

        Order completedOrder = createOrder(OrderStatus.COMPLETED, service);
        completedOrderItem = completedOrder.getOrderItems().get(0);

        Order pendingOrder = createOrder(OrderStatus.PENDING, service);
        pendingOrderItem = pendingOrder.getOrderItems().get(0);
    }

    private Order createOrder(OrderStatus status, Service service) {
        OrderItem item = OrderItem.builder().service(service).priceAtOrder(service.getPrice()).build();
        Order order = Order.builder()
                .customer(customer)
                .status(status)
                .paymentStatus(status == OrderStatus.COMPLETED ? PaymentStatus.PAID : PaymentStatus.UNPAID)
                .totalPrice(service.getPrice())
                .orderItems(List.of(item))
                .build();
        item.setOrder(order);
        return orderRepository.save(order);
    }

    private String generateJwtToken(String email) {
        return jwtService.generateToken(userDetailsService.loadUserByUsername(email));
    }

    @Test
    void createReview_whenOrderIsCompletedAndUserIsCustomer_shouldSucceed() {
        // --- ARRANGE ---
        String customerToken = generateJwtToken(customer.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);

        ReviewRequest requestBody = new ReviewRequest(5, "Layanan yang luar biasa!");
        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse<ReviewResponse>> response = restTemplate.exchange(
                "/api/reviews/order-item/" + completedOrderItem.getId(),
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getRating()).isEqualTo(5);
        assertThat(response.getBody().getData().getComment()).isEqualTo("Layanan yang luar biasa!");

        assertThat(reviewRepository.count()).isEqualTo(1);
        Review savedReview = reviewRepository.findAll().get(0);
        assertThat(savedReview.getOrderItem().getId()).isEqualTo(completedOrderItem.getId());
        assertThat(savedReview.getCustomer().getId()).isEqualTo(customer.getId());

        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void createReview_whenOrderIsNotCompleted_shouldFail() {
        // --- ARRANGE ---
        String customerToken = generateJwtToken(customer.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(customerToken);

        ReviewRequest requestBody = new ReviewRequest(5, "Komentar prematur");
        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/reviews/order-item/" + pendingOrderItem.getId(),
                HttpMethod.POST,
                requestEntity,
                ApiResponse.class
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        assertThat(reviewRepository.count()).isZero();
    }

    @Test
    void createReview_whenUserIsNotCustomer_shouldFailWithForbidden() {
        // --- ARRANGE ---
        String providerToken = generateJwtToken(provider.getEmail());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(providerToken);

        ReviewRequest requestBody = new ReviewRequest(1, "Saya mencoba mereview pesanan saya sendiri");
        HttpEntity<ReviewRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/reviews/order-item/" + completedOrderItem.getId(),
                HttpMethod.POST,
                requestEntity,
                ApiResponse.class
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(reviewRepository.count()).isZero();
    }
}