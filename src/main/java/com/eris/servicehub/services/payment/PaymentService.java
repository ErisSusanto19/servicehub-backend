package com.eris.servicehub.services.payment;

import com.eris.servicehub.dtos.payment.PaymentResponse;
import java.util.Map;
import java.util.UUID;

public interface PaymentService {
    PaymentResponse createTransaction(UUID orderId);
    void handleNotification(Map<String, Object> notificationPayload);
}