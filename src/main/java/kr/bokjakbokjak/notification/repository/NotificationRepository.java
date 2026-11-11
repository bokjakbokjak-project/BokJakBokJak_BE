package kr.bokjakbokjak.notification.repository;

import java.time.Instant;
import kr.bokjakbokjak.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Slice<Notification> findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            Long receiverId, Instant since, Pageable pageable);

    Slice<Notification> findAllByReceiverIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
            Long receiverId, Instant since, Pageable pageable);

    long countByReceiverIdAndReadFalseAndCreatedAtAfter(Long receiverId, Instant since);
}
