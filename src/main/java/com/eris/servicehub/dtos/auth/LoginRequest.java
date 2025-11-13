package com.eris.servicehub.dtos.auth;

public record LoginRequest(
        String email,
        String password
) {}
