package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;
import com.eris.servicehub.dtos.ordernote.OrderNoteRequest;
import com.eris.servicehub.dtos.ordernote.OrderNoteResponse;
import com.eris.servicehub.dtos.payment.PaymentResponse;
import com.eris.servicehub.services.order.OrderService;
import com.eris.servicehub.services.ordernote.OrderNoteService;
import com.eris.servicehub.services.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderNoteService orderNoteService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse data = orderService.createOrder(request);
        ApiResponse<OrderResponse> response = ApiResponse.success(data, "Order created successfully");
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders() {
        List<OrderResponse> data = orderService.getMyOrders();
        ApiResponse<List<OrderResponse>> response = ApiResponse.success(data, "User's orders retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/provider")
    @PreAuthorize("hasAuthority('PROVIDER')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersForProvider() {
        List<OrderResponse> data = orderService.getOrdersForProvider();
        ApiResponse<List<OrderResponse>> response = ApiResponse.success(data, "Provider's orders retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('PROVIDER') and @orderSecurity.isProviderForOrder(authentication, #orderId)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        OrderResponse data = orderService.updateOrderStatus(orderId, request);
        ApiResponse<OrderResponse> response = ApiResponse.success(data, "Order status updated successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('CUSTOMER') and @orderSecurity.isCustomerForOrder(authentication, #orderId)")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable UUID orderId) {
        OrderResponse data = orderService.cancelOrder(orderId);
        ApiResponse<OrderResponse> response = ApiResponse.success(data, "Order cancelled successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/notes")
    @PreAuthorize("@orderSecurity.isParticipant(authentication, #orderId)")
    public ResponseEntity<ApiResponse<List<OrderNoteResponse>>> getOrderNotes(@PathVariable UUID orderId) {
        List<OrderNoteResponse> data = orderNoteService.getNotesForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(data, "Order notes retrieved successfully"));
    }

    @PostMapping("/{orderId}/notes")
    @PreAuthorize("@orderSecurity.isParticipant(authentication, #orderId)")
    public ResponseEntity<ApiResponse<OrderNoteResponse>> addOrderNote(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderNoteRequest request
    ) {
        OrderNoteResponse data = orderNoteService.createNoteForOrder(orderId, request);
        return new ResponseEntity<>(ApiResponse.success(data, "Note added successfully"), HttpStatus.CREATED);
    }

    @PostMapping("/{orderId}/pay")
    @PreAuthorize("hasAuthority('CUSTOMER') and @orderSecurity.isCustomerForOrder(authentication, #orderId)")
    public ResponseEntity<ApiResponse<PaymentResponse>> payForOrder(@PathVariable UUID orderId) {
        PaymentResponse data = paymentService.createTransaction(orderId);
        return ResponseEntity.ok(ApiResponse.success(data, "Payment transaction created successfully"));
    }

    @GetMapping("/{orderId}/payment-status")
    @PreAuthorize("@orderSecurity.isParticipant(authentication, #orderId)")
    public ResponseEntity<ApiResponse<Map<String, String>>> getOrderPaymentStatus(@PathVariable UUID orderId) {
        Map<String, String> status = orderService.getOrderPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(status, "Payment status retrieved successfully"));
    }
}