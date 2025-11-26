package com.eris.servicehub.controllers;

import com.eris.servicehub.AbstractIntegrationTest;
import com.eris.servicehub.config.TestConfig;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.notification.NotificationResponse;
import com.eris.servicehub.entities.Notification;
import com.eris.servicehub.entities.Role;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.repositories.NotificationRepository;
import com.eris.servicehub.repositories.RoleRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Import(TestConfig.class)
public class NotificationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private NotificationRepository notificationRepository;

    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private UserDetailsService userDetailsService;

    private User user1;
    private User user2;
    private Notification user1Notification;
    private String user1Token;
    private String user2Token;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role customerRole = roleRepository.save(Role.builder().name("CUSTOMER").build());

        user1 = createUser("user1@test.com", "User One", customerRole);
        user2 = createUser("user2@test.com", "User Two", customerRole);

        user1Token = generateJwtToken(user1.getEmail());
        user2Token = generateJwtToken(user2.getEmail());

        user1Notification = notificationRepository.save(Notification.builder()
                .user(user1)
                .message("This is a notification for user 1.")
                .isRead(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user2)
                .message("This is a notification for user 2.")
                .isRead(false)
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
    void getMyNotifications_whenLoggedIn_shouldReturnOnlyOwnNotifications() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // --- ACT ---
        ResponseEntity<ApiResponse<List<NotificationResponse>>> response = restTemplate.exchange(
                "/api/notifications",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        List<NotificationResponse> notifications = response.getBody().getData();

        assertThat(notifications).hasSize(1);

        assertThat(notifications.get(0).getMessage()).isEqualTo("This is a notification for user 1.");
    }

    @Test
    void markNotificationAsRead_whenUserIsOwner_shouldSucceed() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user1Token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        assertThat(user1Notification.isRead()).isFalse();

        // --- ACT ---
        ResponseEntity<ApiResponse<NotificationResponse>> response = restTemplate.exchange(
                "/api/notifications/" + user1Notification.getId() + "/read",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().isRead()).isTrue();

        Notification updatedNotification = notificationRepository.findById(user1Notification.getId()).orElseThrow();
        assertThat(updatedNotification.isRead()).isTrue();
    }

    @Test
    void markNotificationAsRead_whenUserIsNotOwner_shouldFail() {
        // --- ARRANGE ---
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user2Token);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        // --- ACT ---
        ResponseEntity<ApiResponse> response = restTemplate.exchange(
                "/api/notifications/" + user1Notification.getId() + "/read",
                HttpMethod.POST,
                requestEntity,
                ApiResponse.class
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        Notification originalNotification = notificationRepository.findById(user1Notification.getId()).orElseThrow();
        assertThat(originalNotification.isRead()).isFalse();
    }
}