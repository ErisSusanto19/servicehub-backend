package com.eris.servicehub.dtos.notification;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class NotificationResponse {
    private UUID id;
    private String message;
    private String link;
    private boolean isRead;
    private Instant createdAt;
}