package com.eris.servicehub.services.auth.implementation;

import com.eris.servicehub.dtos.auth.AuthResponse;
import com.eris.servicehub.dtos.auth.LoginRequest;
import com.eris.servicehub.dtos.auth.RegisterRequest;
import com.eris.servicehub.entities.Profile;
import com.eris.servicehub.entities.Role;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.RoleRepository;
import com.eris.servicehub.repositories.UserRepository;
import com.eris.servicehub.services.security.JwtService;
import com.eris.servicehub.services.auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

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

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: CUSTOMER"));

        var userEntity = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(new HashSet<>(Set.of(customerRole)))
                .enabled(true)
                .build();

        Profile newProfile = new Profile();
        newProfile.setUser(userEntity);
        userEntity.setProfile(newProfile);

        User savedUser = userRepository.save(userEntity);

        userRepository.save(userEntity);

        var userDetails = new org.springframework.security.core.userdetails.User(
                savedUser.getEmail(),
                savedUser.getPassword(),
                savedUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );

        var jwtToken = jwtService.generateToken(userDetails);

        return new AuthResponse(jwtToken);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        var userEntity = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.email()));

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
