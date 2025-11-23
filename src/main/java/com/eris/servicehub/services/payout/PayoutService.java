package com.eris.servicehub.services.payout;

import com.eris.servicehub.dtos.payout.PayoutResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PayoutService {
    PayoutResponse processProviderPayout(UUID providerId);
    Page<PayoutResponse> getMyPayoutHistory(Pageable pageable);
}
