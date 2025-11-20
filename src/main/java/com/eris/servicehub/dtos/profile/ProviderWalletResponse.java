package com.eris.servicehub.dtos.profile;

import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ProviderWalletResponse {
    private BigDecimal totalGrossRevenue;
    private BigDecimal totalNetRevenue;
    private BigDecimal pendingPayout;
    private List<TransactionSummary> recentTransactions;

    @Data
    @Builder
    public static class TransactionSummary {
        private UUID orderId;
        private Instant completedAt;
        private BigDecimal grossAmount;
        private BigDecimal platformFee;
        private BigDecimal netAmount;
        private PaymentStatus paymentStatus;
    }
}