package com.eris.servicehub.controllers;

import com.eris.servicehub.AbstractIntegrationTest;
import com.eris.servicehub.dtos.auth.AuthResponse;
import com.eris.servicehub.dtos.auth.LoginRequest;
import com.eris.servicehub.dtos.auth.RegisterRequest;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void registerAndLogin_shouldSucceed_whenCredentialsAreValid() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Test User",
                "test@example.com",
                "password123"
        );

        ResponseEntity<ApiResponse<AuthResponse>> registerResponse = restTemplate.exchange(
                "/auth/register",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(registerRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getStatus()).isEqualTo("success");
        assertThat(registerResponse.getBody().getData().token()).isNotBlank();

        User createdUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(createdUser.getName()).isEqualTo("Test User");

        LoginRequest loginRequest = new LoginRequest(
                "test@example.com",
                "password123"
        );

        ResponseEntity<ApiResponse<AuthResponse>> loginResponse = restTemplate.exchange(
                "/auth/login",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(loginRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getData().token()).isNotBlank();
    }

    @Test
    void register_shouldFail_whenEmailIsAlreadyTaken() {

        RegisterRequest initialRequest = new RegisterRequest("User A", "duplicate@example.com", "pass1");
        restTemplate.postForEntity("/auth/register", initialRequest, ApiResponse.class);

        RegisterRequest duplicateRequest = new RegisterRequest("User B", "duplicate@example.com", "pass2");
        ResponseEntity<ApiResponse> response = restTemplate.postForEntity("/auth/register", duplicateRequest, ApiResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

    }
}