package com.eris.servicehub.dtos.service;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ServiceResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Double averageRating;
    private ProviderSummary provider;
    private CategorySummary category;

    @Data
    @Builder
    public static class ProviderSummary {
        private UUID id;
        private String name;
    }

    @Data
    @Builder
    public static class CategorySummary {
        private UUID id;
        private String name;
    }
}