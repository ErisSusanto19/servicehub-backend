package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.admin.UserSummaryResponse;
import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.common.PagedResponse;
import com.eris.servicehub.dtos.order.OrderResponse;
import com.eris.servicehub.services.admin.AdminService;
import com.eris.servicehub.services.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @Autowired
    private OrderService orderService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<UserSummaryResponse>>> getAllUsers(Pageable pageable) {
        Page<UserSummaryResponse> userPage = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(userPage), "Users retrieved successfully"));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> banUser(@PathVariable UUID userId) {
        UserSummaryResponse data = adminService.setUserEnabledStatus(userId, false);
        return ResponseEntity.ok(ApiResponse.success(data, "User banned successfully"));
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> unbanUser(@PathVariable UUID userId) {
        UserSummaryResponse data = adminService.setUserEnabledStatus(userId, true);
        return ResponseEntity.ok(ApiResponse.success(data, "User unbanned successfully"));
    }

    @PostMapping("/orders/{orderId}/confirm-payment")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrderPayment(@PathVariable UUID orderId) {
        OrderResponse data = orderService.confirmPayment(orderId);
        return ResponseEntity.ok(ApiResponse.success(data, "Order payment confirmed successfully"));
    }
}