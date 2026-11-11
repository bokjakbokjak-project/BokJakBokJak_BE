package kr.bokjakbokjak.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import kr.bokjakbokjak.notification.entity.Notification;
import kr.bokjakbokjak.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationRepositoryTest {
    private static final Long RECEIVER = 7L;

    @Autowired NotificationRepository notificationRepository;
    @Autowired EntityManager em;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void listsRecentNotificationsNewestFirst() {
        Notification older = notificationRepository.save(chatRequested(RECEIVER, 45L));
        Notification newer = notificationRepository.save(
                Notification.create(RECEIVER, 52L, NotificationType.PARTNER_CONFIRMED, "새벽감성님이 먼저 매칭을 확정했어요."));
        notificationRepository.saveAndFlush(chatRequested(12L, 46L));
        Instant since = Instant.now().minus(Duration.ofDays(30));

        Slice<Notification> page = notificationRepository
                .findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(RECEIVER, since, PageRequest.of(0, 1));

        assertThat(page.getContent()).extracting(Notification::getId).containsExactly(newer.getId());
        assertThat(page.hasNext()).isTrue();
        assertThat(notificationRepository
                .findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(RECEIVER, since, PageRequest.of(1, 1))
                .getContent()).extracting(Notification::getId).containsExactly(older.getId());
    }

    @Test
    void excludesNotificationsBeforeSince() {
        notificationRepository.saveAndFlush(chatRequested(RECEIVER, 45L));

        assertThat(notificationRepository.findAllByReceiverIdAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                RECEIVER, Instant.now().plusSeconds(60), PageRequest.of(0, 20))).isEmpty();
        assertThat(notificationRepository.countByReceiverIdAndReadFalseAndCreatedAtAfter(
                RECEIVER, Instant.now().plusSeconds(60))).isZero();
    }

    @Test
    void filtersAndCountsUnread() {
        Notification read = notificationRepository.save(chatRequested(RECEIVER, 45L));
        Notification unread = notificationRepository.saveAndFlush(chatRequested(RECEIVER, 46L));
        em.createQuery("update Notification n set n.read = true where n.id = :id")
                .setParameter("id", read.getId())
                .executeUpdate();
        em.clear();
        Instant since = Instant.now().minus(Duration.ofDays(30));

        assertThat(notificationRepository.findAllByReceiverIdAndReadFalseAndCreatedAtAfterOrderByCreatedAtDescIdDesc(
                RECEIVER, since, PageRequest.of(0, 20)))
                .extracting(Notification::getId).containsExactly(unread.getId());
        assertThat(notificationRepository.countByReceiverIdAndReadFalseAndCreatedAtAfter(RECEIVER, since))
                .isEqualTo(1);
    }

    private Notification chatRequested(Long receiverId, Long matchingId) {
        return Notification.create(receiverId, matchingId, NotificationType.CHAT_REQUESTED, "민트초코님과의 매칭이 시작되었어요");
    }
}
