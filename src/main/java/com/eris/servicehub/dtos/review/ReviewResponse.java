package com.eris.servicehub.dtos.review;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private int rating;
    private String comment;
    private CustomerSummary customer;
    private Instant createdAt;

    @Data
    @Builder
    public static class CustomerSummary {
        private UUID id;
        private String name;
    }
}