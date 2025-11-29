package com.eris.servicehub.controllers;

import com.eris.servicehub.AbstractIntegrationTest;
import com.eris.servicehub.config.TestConfig;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.profile.ProviderDashboardResponse;
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
public class ProfileControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;

    private User provider1;
    private String provider1Token;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        orderRepository.deleteAll();
        serviceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role providerRole = roleRepository.save(Role.builder().name("PROVIDER").build());
        User customer = userRepository.save(User.builder().name("Cust").email("c@c.com").password("p").roles(Set.of()).build());
        provider1 = createUser("provider1@test.com", "Provider One", providerRole);
        User provider2 = createUser("provider2@test.com", "Provider Two", providerRole);
        provider1Token = generateJwtToken(provider1.getEmail());

        Category cat = categoryRepository.save(Category.builder().name("Cat").build());
        Service service1 = createService(provider1, cat, "100.00");
        Service service2 = createService(provider2, cat, "50.00");

        createOrder(customer, service1, OrderStatus.COMPLETED, new BigDecimal("100.00"), new BigDecimal("90.00")); // Revenue
        createOrder(customer, service1, OrderStatus.IN_PROGRESS, new BigDecimal("100.00"), BigDecimal.ZERO); // Active
        createOrder(customer, service1, OrderStatus.PENDING, new BigDecimal("100.00"), BigDecimal.ZERO); // Ignored

        Order completedOrder1 = createOrder(customer, service1, OrderStatus.COMPLETED, new BigDecimal("100.00"), new BigDecimal("90.00"));
        Order completedOrder2 = createOrder(customer, service1, OrderStatus.COMPLETED, new BigDecimal("100.00"), new BigDecimal("90.00"));
        createReview(customer, completedOrder1.getOrderItems().get(0), 4);
        createReview(customer, completedOrder2.getOrderItems().get(0), 5);

        createOrder(customer, service2, OrderStatus.COMPLETED, new BigDecimal("50.00"), new BigDecimal("45.00"));
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

    @Test
    void getProviderDashboard_shouldReturnCorrectAggregatedData() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(provider1Token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // --- ACT ---
        ResponseEntity<ApiResponse<ProviderDashboardResponse>> response = restTemplate.exchange(
                "/api/profile/me/dashboard", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {});

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ProviderDashboardResponse dashboard = response.getBody().getData();

        assertThat(dashboard.getTotalGrossRevenue()).isEqualByComparingTo("300.00");
        assertThat(dashboard.getTotalNetRevenue()).isEqualByComparingTo("270.00");
        assertThat(dashboard.getActiveOrdersCount()).isEqualTo(1);
        assertThat(dashboard.getCompletedOrdersCount()).isEqualTo(3);
        assertThat(dashboard.getAverageRating()).isEqualTo(4.5);
    }

    private Service createService(User p, Category c, String price) {
        Service service = Service.builder()
                .name("Service by " + p.getName())
                .price(new BigDecimal(price))
                .provider(p)
                .category(c)
                .build();
        return serviceRepository.save(service);
    }

    private Order createOrder(User c, Service s, OrderStatus st, BigDecimal gross, BigDecimal net) {
        OrderItem item = OrderItem.builder()
                .service(s)
                .priceAtOrder(s.getPrice())
                .build();

        Order order = Order.builder()
                .customer(c)
                .status(st)
                .paymentStatus(st == OrderStatus.COMPLETED ? PaymentStatus.PAID : PaymentStatus.UNPAID)
                .totalPrice(gross)
                .netPayout(net)
                .platformFee(gross.subtract(net))
                .orderItems(List.of(item))
                .build();

        item.setOrder(order);

        return orderRepository.save(order);
    }

    private void createReview(User c, OrderItem oi, int rating) {
        Review review = Review.builder()
                .customer(c)
                .orderItem(oi)
                .service(oi.getService())
                .rating(rating)
                .comment("A test review.")
                .build();
        reviewRepository.save(review);
    }
}