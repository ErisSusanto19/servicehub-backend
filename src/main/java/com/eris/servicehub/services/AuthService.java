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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

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

    @Autowired
    private JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        var userEntity = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(customerRole))
                .build();

        userRepository.save(userEntity);

        var userDetails = new org.springframework.security.core.userdetails.User(
                userEntity.getEmail(),
                userEntity.getPassword(),
                userEntity.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );

        var jwtToken = jwtService.generateToken(userDetails);

        return new AuthResponse(jwtToken);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var userEntity = userRepository.findByEmail(request.email())
                .orElseThrow();

        var userDetails = new org.springframework.security.core.userdetails.User(
                userEntity.getEmail(),
                userEntity.getPassword(),
                userEntity.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );

        var jwtToken = jwtService.generateToken(userDetails);

        return new AuthResponse(jwtToken);
    }
}
