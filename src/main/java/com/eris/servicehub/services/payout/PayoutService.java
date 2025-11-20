package com.eris.servicehub.services.payout;

import com.eris.servicehub.dtos.payout.PayoutResponse;

import java.util.List;
import java.util.UUID;

public interface PayoutService {
    PayoutResponse processProviderPayout(UUID providerId);
    List<PayoutResponse> getMyPayoutHistory();
}
