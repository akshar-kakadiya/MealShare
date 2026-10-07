package com.mealshare.notification.dto;

import com.mealshare.notification.Notification;
import com.mealshare.notification.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String entityType,
        UUID entityId,
        boolean read,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponse fromEntity(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getEntityType(),
                n.getEntityId(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
