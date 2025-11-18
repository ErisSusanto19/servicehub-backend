package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.order.OrderRequest;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.dtos.order.UpdateOrderStatusRequest;
import com.eris.servicehub.services.order.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

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
}