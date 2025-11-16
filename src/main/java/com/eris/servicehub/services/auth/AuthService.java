package com.eris.servicehub.services.auth;

import com.eris.servicehub.dtos.auth.AuthResponse;
import com.eris.servicehub.dtos.auth.LoginRequest;
import com.eris.servicehub.dtos.auth.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
