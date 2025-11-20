package com.eris.servicehub.dtos.payout;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PayoutResponse {
    private UUID id;
    private UUID providerId;
    private BigDecimal totalAmount;
    private Instant payoutDate;
    private int numberOfOrders;
    private List<OrderItemSummary> orders;

    @Data
    @Builder
    public static class OrderItemSummary {
        private UUID orderId;
        private BigDecimal netAmount;
        private Instant completedDate;
    }
}