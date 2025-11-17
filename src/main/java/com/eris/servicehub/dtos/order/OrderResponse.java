package com.eris.servicehub.dtos.order;

import com.eris.servicehub.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private UUID id;
    private CustomerSummary customer;
    private List<OrderItemResponse> orderItems;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private Instant createdAt;

    @Data
    @Builder
    public static class CustomerSummary {
        private UUID id;
        private String name;
    }

    @Data
    @Builder
    public static class OrderItemResponse {
        private UUID id;
        private ServiceSummary service;
        private BigDecimal priceAtOrder;
    }

    @Data
    @Builder
    public static class ServiceSummary {
        private UUID id;
        private String name;
        private ProviderSummary provider;
    }

    @Data
    @Builder
    public static class ProviderSummary {
        private UUID id;
        private String name;
    }
}