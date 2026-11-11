package kr.bokjakbokjak.notification.service;

import java.time.Duration;
import java.time.Instant;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.notification.dto.NotificationListResponse;
import kr.bokjakbokjak.notification.dto.NotificationReadResponse;
import kr.bokjakbokjak.notification.dto.NotificationResponse;
import kr.bokjakbokjak.notification.dto.UnreadCountResponse;
import kr.bokjakbokjak.notification.entity.Notification;
import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService implements NotificationSender {
    private static final Duration RETENTION = Duration.ofDays(30);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void send(Long receiverId, Long matchingId, NotificationType type, String content) {
        notificationRepository.save(Notification.create(receiverId, matchingId, type, content));
    }

    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(Long userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Instant since = retentionStart();
        Slice<Notification> slice = unreadOnly
                ? notificationRepository.findAllByReceiverIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                        userId, since, pageable)
                : notificationRepository.findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                        userId, since, pageable);
        return new NotificationListResponse(
                slice.getContent().stream().map(NotificationResponse::from).toList(),
                page,
                slice.hasNext());
    }

    @Transactional
    public NotificationReadResponse markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND));
        if (!notification.isReceivedBy(userId)) {
            throw new ApiException(ErrorCode.NOTIFICATION_FORBIDDEN);
        }
        notification.markRead();
        return new NotificationReadResponse(notification.getId(), notification.isRead());
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(Long userId) {
        return new UnreadCountResponse(
                notificationRepository.countByReceiverIdAndReadFalseAndCreatedAtAfter(userId, retentionStart()));
    }

    private Instant retentionStart() {
        return Instant.now().minus(RETENTION);
    }
}
