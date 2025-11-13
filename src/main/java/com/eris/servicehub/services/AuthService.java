package com.eris.servicehub.services;

import com.eris.servicehub.dtos.auth.AuthResponse;
import com.eris.servicehub.dtos.auth.LoginRequest;
import com.eris.servicehub.dtos.auth.RegisterRequest;
import com.eris.servicehub.entities.Role;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.repositories.RoleRepository;
import com.eris.servicehub.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest request) {

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        var user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(customerRole))
                .build();

        userRepository.save(user);

        return new AuthResponse("dummy-jwt-token-for-now");
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var user = userRepository.findByEmail(request.email())
                .orElseThrow();

        return new AuthResponse("dummy-jwt-token-for-now");
    }
}
