package com.eris.servicehub.services.notification;

import com.eris.servicehub.dtos.notification.NotificationResponse;
import com.eris.servicehub.entities.User;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    void createNotification(User user, String message, String link);
    List<NotificationResponse> getMyNotifications();
    NotificationResponse markAsRead(UUID notificationId);
}
