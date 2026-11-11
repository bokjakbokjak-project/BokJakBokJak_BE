package kr.bokjakbokjak.notification.dto;

import java.time.Instant;
import kr.bokjakbokjak.notification.entity.Notification;
import kr.bokjakbokjak.notification.entity.NotificationType;

public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String content,
        Long matchingId,
        boolean isRead,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.getMatchingId(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
