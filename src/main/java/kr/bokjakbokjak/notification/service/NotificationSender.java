package kr.bokjakbokjak.notification.service;

import kr.bokjakbokjak.notification.entity.NotificationType;

public interface NotificationSender {
    void send(Long receiverId, Long matchingId, NotificationType type, String content);
}
