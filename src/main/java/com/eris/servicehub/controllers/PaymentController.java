package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.services.payment.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/midtrans-notification")
    public ResponseEntity<ApiResponse<Void>> handleMidtransNotification(@RequestBody Map<String, Object> payload) {
        paymentService.handleNotification(payload);
        return new ResponseEntity<>(ApiResponse.success(null, "Notification processed"), HttpStatus.OK);
    }
}