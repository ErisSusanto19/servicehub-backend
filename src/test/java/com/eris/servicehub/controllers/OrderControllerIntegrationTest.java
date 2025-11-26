package com.eris.servicehub.controllers;

import com.eris.servicehub.AbstractIntegrationTest;
import com.eris.servicehub.config.TestConfig;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;
import com.eris.servicehub.dtos.ordernote.OrderNoteRequest;
import com.eris.servicehub.entities.*;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import com.eris.servicehub.repositories.*;
import com.eris.servicehub.services.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    @Autowired private OrderNoteRepository orderNoteRepository;

    private User customer;
    private User provider;
    private Service serviceToOrder;
    private Order existingOrder;

    private String customerToken;
    private String providerToken;

    @BeforeEach
    void globalSetUp() {
        orderNoteRepository.deleteAll();
        notificationRepository.deleteAll();
        orderRepository.deleteAll();
        serviceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role customerRole = roleRepository.save(Role.builder().name("CUSTOMER").build());
        Role providerRole = roleRepository.save(Role.builder().name("PROVIDER").build());

        customer = createUser("customer@test.com", "Test Customer", customerRole);
        provider = createUser("provider@test.com", "Test Provider", providerRole);

        customerToken = generateJwtToken(customer.getEmail());
        providerToken = generateJwtToken(provider.getEmail());

        Category category = categoryRepository.save(Category.builder().name("Test Category").build());
        serviceToOrder = Service.builder()
                .name("Test Service")
                .price(new BigDecimal("100.00"))
                .provider(provider)
                .category(category)
                .build();
        serviceRepository.save(serviceToOrder);
    }

    private User createUser(String email, String name, Role role) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode("password"))
                .roles(Set.of(role))
                .build();
        return userRepository.save(user);
    }

    private String generateJwtToken(String email) {
        return jwtService.generateToken(userDetailsService.loadUserByUsername(email));
    }

    @Nested
    @DisplayName("Tests for an Existing Order (PATCH, POST Notes, etc.)")
    class ExistingOrderTests {

        private Order existingOrder;

        @BeforeEach
        void setUpExistingOrder() {
            existingOrder = Order.builder()
                    .customer(customer)
                    .totalPrice(new BigDecimal("100.00"))
                    .status(OrderStatus.PENDING)
                    .paymentStatus(PaymentStatus.PAID)
                    .orderItems(List.of(
                            OrderItem.builder().service(serviceToOrder).priceAtOrder(serviceToOrder.getPrice()).build()
                    ))
                    .build();
            existingOrder.getOrderItems().forEach(item -> item.setOrder(existingOrder));
            orderRepository.save(existingOrder);
        }

        @Test
        void updateOrderStatus_whenUserIsProviderForOrder_shouldSucceed() {
            // Arrange
            UpdateOrderStatusRequest requestBody = new UpdateOrderStatusRequest(OrderStatus.ACCEPTED);
            HttpEntity<UpdateOrderStatusRequest> requestEntity = new HttpEntity<>(requestBody, createAuthHeaders(providerToken));

            // Act
            ResponseEntity<ApiResponse<OrderResponse>> response = restTemplate.exchange(
                    "/api/orders/" + existingOrder.getId() + "/status", HttpMethod.PATCH, requestEntity, new ParameterizedTypeReference<>() {});

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            Order updatedOrder = orderRepository.findById(existingOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.ACCEPTED);
        }

        @Test
        void addOrderNote_whenUserIsParticipant_shouldCreateNoteAndNotifyOtherParty() {
            // Arrange
            OrderNoteRequest requestBody = new OrderNoteRequest("Catatan dari customer.");
            HttpEntity<OrderNoteRequest> requestEntity = new HttpEntity<>(requestBody, createAuthHeaders(customerToken));

            // Act
            ResponseEntity<ApiResponse> response = restTemplate.exchange(
                    "/api/orders/" + existingOrder.getId() + "/notes", HttpMethod.POST, requestEntity, ApiResponse.class);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            List<Notification> providerNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(provider.getId());
            assertThat(providerNotifications).hasSize(1);
        }
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}