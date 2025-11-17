package com.eris.servicehub.dtos.order;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record OrderRequest(
        @NotEmpty(message = "Service IDs cannot be empty")
        List<UUID> serviceIds
) {}