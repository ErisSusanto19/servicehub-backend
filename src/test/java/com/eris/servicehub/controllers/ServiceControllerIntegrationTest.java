package com.eris.servicehub.controllers;

import com.eris.servicehub.AbstractIntegrationTest;
import com.eris.servicehub.config.TestConfig;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.service.ServiceRequest;
import com.eris.servicehub.dtos.service.ServiceResponse;
import com.eris.servicehub.entities.Category;
import com.eris.servicehub.entities.Role;
import com.eris.servicehub.entities.Service;
import com.eris.servicehub.entities.User;
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
public class ServiceControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ServiceRepository serviceRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;

    private User provider1;
    private User provider2;
    private Category category;
    private Service serviceOfProvider1;
    private String provider1Token;
    private String provider2Token;

    @BeforeEach
    void setUp() {
        serviceRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role providerRole = roleRepository.save(Role.builder().name("PROVIDER").build());

        provider1 = createUser("provider1@test.com", "Provider One", providerRole);
        provider2 = createUser("provider2@test.com", "Provider Two", providerRole);

        provider1Token = generateJwtToken(provider1.getEmail());
        provider2Token = generateJwtToken(provider2.getEmail());

        category = categoryRepository.save(Category.builder().name("Test Category").description("Desc").build());

        serviceOfProvider1 = serviceRepository.save(Service.builder()
                .name("Service Milik Provider 1")
                .description("Initial Desc")
                .price(new BigDecimal("50.00"))
                .provider(provider1)
                .category(category)
                .build());
    }

    private User createUser(String email, String name, Role role) {
        User user = User.builder().name(name).email(email).password(passwordEncoder.encode("password")).roles(Set.of(role)).build();
        return userRepository.save(user);
    }

    private String generateJwtToken(String email) {
        return jwtService.generateToken(userDetailsService.loadUserByUsername(email));
    }

    @Test
    void createService_whenUserIsProvider_shouldSucceed() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(provider1Token);

        ServiceRequest requestBody = new ServiceRequest();
        requestBody.setName("Jasa Baru");
        requestBody.setDescription("Deskripsi Jasa Baru");
        requestBody.setPrice(new BigDecimal("150.00"));
        requestBody.setCategoryId(category.getId());

        HttpEntity<ServiceRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse<ServiceResponse>> response = restTemplate.exchange(
                "/api/services",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getName()).isEqualTo("Jasa Baru");

        assertThat(serviceRepository.count()).isEqualTo(2);
    }

    @Test
    void updateService_whenUserIsOwner_shouldSucceed() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(provider1Token);

        ServiceRequest requestBody = new ServiceRequest();
        requestBody.setName("Nama Service Diubah");
        requestBody.setDescription("Deskripsi Diubah");
        requestBody.setPrice(new BigDecimal("99.00"));
        requestBody.setCategoryId(category.getId());

        HttpEntity<ServiceRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse<ServiceResponse>> response = restTemplate.exchange(
                "/api/services/" + serviceOfProvider1.getId(),
                HttpMethod.PUT,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getName()).isEqualTo("Nama Service Diubah");

        Service updatedService = serviceRepository.findById(serviceOfProvider1.getId()).orElseThrow();
        assertThat(updatedService.getPrice()).isEqualByComparingTo("99.00");
    }

    @Test
    void updateService_whenUserIsNotOwner_shouldFailWithForbidden() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(provider2Token);

        ServiceRequest requestBody = new ServiceRequest();
        requestBody.setName("Mencoba Mengubah Nama");
        requestBody.setCategoryId(category.getId());
        requestBody.setPrice(new BigDecimal("1.00"));

        HttpEntity<ServiceRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        // --- ACT ---
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/services/" + serviceOfProvider1.getId(),
                HttpMethod.PUT,
                requestEntity,
                ApiResponse.class
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Service serviceInDb = serviceRepository.findById(serviceOfProvider1.getId()).orElseThrow();
        assertThat(serviceInDb.getName()).isEqualTo("Service Milik Provider 1");
    }

    @Test
    void deleteService_whenUserIsOwner_shouldSucceed() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(provider1Token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // --- ACT ---
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/services/" + serviceOfProvider1.getId(),
                HttpMethod.DELETE,
                requestEntity,
                ApiResponse.class
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(serviceRepository.findById(serviceOfProvider1.getId())).isEmpty();
    }
}