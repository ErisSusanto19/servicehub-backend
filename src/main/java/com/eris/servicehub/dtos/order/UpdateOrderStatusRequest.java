package com.eris.servicehub.dtos.order;

import com.eris.servicehub.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Status cannot be null")
        OrderStatus status
) {}