package com.eris.servicehub.services.payment.implementation;

import com.eris.servicehub.dtos.payment.PaymentResponse;
import com.eris.servicehub.entities.Order;
import com.eris.servicehub.entities.User;
import com.eris.servicehub.enums.OrderStatus;
import com.eris.servicehub.enums.PaymentStatus;
import com.eris.servicehub.exceptions.ResourceNotFoundException;
import com.eris.servicehub.repositories.OrderRepository;
import com.eris.servicehub.services.notification.NotificationService;
import com.eris.servicehub.services.payment.PaymentService;
import com.midtrans.httpclient.error.MidtransError;
import com.midtrans.service.MidtransSnapApi;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired private MidtransSnapApi midtransSnapApi;
    @Autowired private OrderRepository orderRepository;
    @Autowired private NotificationService notificationService;

    @Override
    @Transactional
    public PaymentResponse createTransaction(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Order has already been paid.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create payment for a cancelled order.");
        }

        User customer = order.getCustomer();

        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", order.getId().toString());
        transactionDetails.put("gross_amount", order.getTotalPrice().longValue());

        Map<String, String> customerDetails = new HashMap<>();
        customerDetails.put("first_name", customer.getName());
        customerDetails.put("email", customer.getEmail());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("transaction_details", transactionDetails);
        requestBody.put("customer_details", customerDetails);

        try {
            JSONObject response = midtransSnapApi.createTransaction(requestBody);
            String transactionId = (String) transactionDetails.get("order_id");
            String redirectUrl = response.getString("redirect_url");

            order.setTransactionId(transactionId);
            orderRepository.save(order);

            return PaymentResponse.builder()
                    .transactionId(transactionId)
                    .redirectUrl(redirectUrl)
                    .build();
        } catch (MidtransError e) {
            throw new RuntimeException("Midtrans API error: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handleNotification(Map<String, Object> notificationPayload) {
        String orderIdString = (String) notificationPayload.get("order_id");
        UUID orderId = UUID.fromString(orderIdString);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        try {
            // if (!isValid) {
            //     System.out.println("Invalid Midtrans signature");
            //     return;
            // }

            String transactionStatus = (String) notificationPayload.get("transaction_status");
            String fraudStatus = (String) notificationPayload.get("fraud_status");

            if ("capture".equals(transactionStatus)) {
                if ("accept".equals(fraudStatus)) {
                    updateOrderToPaid(order);
                }
            } else if ("settlement".equals(transactionStatus)) {
                    updateOrderToPaid(order);
            } else if ("cancel".equals(transactionStatus) || "deny".equals(transactionStatus) || "expire".equals(transactionStatus)) {
                order.setPaymentStatus(PaymentStatus.FAILED);
            }

            orderRepository.save(order);

        } catch (Exception e) {
            System.err.println("Error processing Midtrans notification: " + e.getMessage());
        }

    }

    private void updateOrderToPaid(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            String message = String.format("Payment for order #%s was successful.", order.getId().toString().substring(0, 8));
            notificationService.createNotification(order.getCustomer(), message, "/customer/orders/" + order.getId());
        }
    }
}