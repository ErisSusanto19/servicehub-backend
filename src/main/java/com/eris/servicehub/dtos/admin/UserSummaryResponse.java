package com.eris.servicehub.dtos.admin;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class UserSummaryResponse {
    private UUID id;
    private String name;
    private String email;
    private Set<String> roles;
    private boolean enabled;
    private Instant createdAt;
}