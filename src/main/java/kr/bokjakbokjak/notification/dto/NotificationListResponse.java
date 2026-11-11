package kr.bokjakbokjak.notification.dto;

import java.util.List;

public record NotificationListResponse(List<NotificationResponse> notifications, int page, boolean hasNext) {
    public NotificationListResponse {
        notifications = List.copyOf(notifications);
    }
}
