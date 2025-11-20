package com.eris.servicehub.controllers;

import com.eris.servicehub.dtos.common.ApiResponse;
import com.eris.servicehub.dtos.notification.NotificationResponse;
import com.eris.servicehub.services.notification.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications() {
        List<NotificationResponse> data = notificationService.getMyNotifications();
        return ResponseEntity.ok(ApiResponse.success(data, "Notifications retrieved successfully"));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markNotificationAsRead(@PathVariable UUID notificationId) {
        NotificationResponse data = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success(data, "Notification marked as read"));
    }
}