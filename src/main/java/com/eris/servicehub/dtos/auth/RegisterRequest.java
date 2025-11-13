package com.eris.servicehub.dtos.auth;

public record RegisterRequest(
        String name,
        String email,
        String password
) {}

