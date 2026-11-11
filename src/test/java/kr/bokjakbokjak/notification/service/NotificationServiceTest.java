package kr.bokjakbokjak.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.notification.dto.NotificationListResponse;
import kr.bokjakbokjak.notification.dto.NotificationResponse;
import kr.bokjakbokjak.notification.entity.Notification;
import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceTest {
    private static final Long RECEIVER = 7L;
    private static final Long OTHER = 12L;

    @Autowired NotificationService notificationService;
    @Autowired NotificationSender notificationSender;
    @Autowired NotificationRepository notificationRepository;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void sentNotificationAppearsInReceiverList() {
        notificationSender.send(RECEIVER, 45L, NotificationType.CHAT_REQUESTED, "민트초코님과의 매칭이 시작되었어요");

        NotificationListResponse response = notificationService.getNotifications(RECEIVER, false, 0, 20);

        assertThat(response.notifications()).singleElement().satisfies(n -> {
            assertThat(n.type()).isEqualTo(NotificationType.CHAT_REQUESTED);
            assertThat(n.content()).isEqualTo("민트초코님과의 매칭이 시작되었어요");
            assertThat(n.matchingId()).isEqualTo(45L);
            assertThat(n.isRead()).isFalse();
            assertThat(n.createdAt()).isNotNull();
        });
        assertThat(response.page()).isZero();
        assertThat(response.hasNext()).isFalse();
        assertThat(notificationService.getNotifications(OTHER, false, 0, 20).notifications()).isEmpty();
    }

    @Test
    void listsNewestFirstWithPaging() {
        Long first = save(RECEIVER, 45L).getId();
        Long second = save(RECEIVER, 46L).getId();
        Long third = save(RECEIVER, 47L).getId();

        NotificationListResponse firstPage = notificationService.getNotifications(RECEIVER, false, 0, 2);
        NotificationListResponse secondPage = notificationService.getNotifications(RECEIVER, false, 1, 2);

        assertThat(firstPage.notifications()).extracting(NotificationResponse::notificationId)
                .containsExactly(third, second);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.notifications()).extracting(NotificationResponse::notificationId)
                .containsExactly(first);
        assertThat(secondPage.page()).isEqualTo(1);
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void unreadOnlyAndUnreadCountExcludeReadNotifications() {
        Long read = save(RECEIVER, 45L).getId();
        Long unread = save(RECEIVER, 46L).getId();
        notificationService.markRead(RECEIVER, read);

        assertThat(notificationService.getNotifications(RECEIVER, true, 0, 20).notifications())
                .extracting(NotificationResponse::notificationId).containsExactly(unread);
        assertThat(notificationService.countUnread(RECEIVER).count()).isEqualTo(1);
    }

    @Test
    void markReadIsIdempotent() {
        Long id = save(RECEIVER, 45L).getId();

        assertThat(notificationService.markRead(RECEIVER, id).isRead()).isTrue();
        assertThat(notificationService.markRead(RECEIVER, id).isRead()).isTrue();
        assertThat(notificationRepository.findById(id)).get().extracting(Notification::isRead).isEqualTo(true);
    }

    @Test
    void cannotReadOthersNotification() {
        Long id = save(RECEIVER, 45L).getId();

        ApiException exception = assertThrows(ApiException.class, () -> notificationService.markRead(OTHER, id));

        assertThat(exception.code()).isEqualTo("NOTIFICATION_FORBIDDEN");
        assertThat(exception.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notificationRepository.findById(id)).get().extracting(Notification::isRead).isEqualTo(false);
    }

    @Test
    void readingMissingNotificationFails() {
        ApiException exception = assertThrows(ApiException.class,
                () -> notificationService.markRead(RECEIVER, Long.MAX_VALUE));

        assertThat(exception.code()).isEqualTo("NOTIFICATION_NOT_FOUND");
        assertThat(exception.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Notification save(Long receiverId, Long matchingId) {
        return notificationRepository.saveAndFlush(Notification.create(
                receiverId, matchingId, NotificationType.CHAT_REQUESTED, "민트초코님과의 매칭이 시작되었어요"));
    }
}
