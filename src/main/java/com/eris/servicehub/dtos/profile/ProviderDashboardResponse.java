package com.eris.servicehub.dtos.profile;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class ProviderDashboardResponse {
    private BigDecimal totalGrossRevenue;
    private BigDecimal totalNetRevenue;
    private BigDecimal pendingPayout;
    private long activeOrdersCount;
    private long completedOrdersCount;
    private Double averageRating;
}