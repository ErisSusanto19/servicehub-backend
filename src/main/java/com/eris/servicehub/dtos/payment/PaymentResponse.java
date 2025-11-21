package com.eris.servicehub.dtos.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String transactionId;
    private String redirectUrl;
}