package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import kr.bokjakbokjak.room.dto.MatchingConfirmResponse;
import kr.bokjakbokjak.room.entity.EndReason;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.entity.MessageType;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class MatchingConfirmServiceTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final long DAWN = 13L;
    private static final long CLOVER = 14L;
    private static final long OUTSIDER = 99L;

    @Autowired MatchingConfirmService matchingConfirmService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JdbcClient jdbc;

    private Long matchingId;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        users.addUser(BOOKMARK, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        users.addUser(DAWN, "새벽감성", 1);
        users.addUser(CLOVER, "네잎클로버", 1);
        matchingId = start(BOOKMARK, MINT);
    }

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAll();
        notificationRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void firstConfirmNotifiesPartner() {
        MatchingConfirmResponse response = matchingConfirmService.confirm(BOOKMARK, matchingId);

        assertThat(response).isEqualTo(new MatchingConfirmResponse(matchingId, MatchingStatus.CHATTING, true, false));
        assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getReceiverId()).isEqualTo(MINT);
            assertThat(notification.getMatchingId()).isEqualTo(matchingId);
            assertThat(notification.getType()).isEqualTo(NotificationType.PARTNER_CONFIRMED);
            assertThat(notification.getContent()).isEqualTo("책갈피님이 먼저 매칭을 확정했어요.");
        });
    }

    @Test
    void repeatedConfirmReturnsCurrentState() {
        matchingConfirmService.confirm(BOOKMARK, matchingId);

        MatchingConfirmResponse again = matchingConfirmService.confirm(BOOKMARK, matchingId);

        assertThat(again).isEqualTo(new MatchingConfirmResponse(matchingId, MatchingStatus.CHATTING, true, false));
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void bothConfirmEndsOtherChatsOfBothUsers() {
        Long bookmarkOther = start(DAWN, BOOKMARK);
        Long mintOther = start(MINT, CLOVER);
        Long unrelated = start(DAWN, CLOVER);
        matchingConfirmService.confirm(BOOKMARK, matchingId);

        MatchingConfirmResponse response = matchingConfirmService.confirm(MINT, matchingId);

        assertThat(response).isEqualTo(new MatchingConfirmResponse(matchingId, MatchingStatus.CONFIRMED, true, true));
        assertThat(matchingRepository.findById(matchingId).orElseThrow().getConfirmedAt()).isNotNull();
        for (Long ended : List.of(bookmarkOther, mintOther)) {
            Matching matching = matchingRepository.findById(ended).orElseThrow();
            assertThat(matching.getStatus()).isEqualTo(MatchingStatus.ENDED);
            assertThat(matching.getEndReason()).isEqualTo(EndReason.PARTNER_MATCHED);
            assertThat(matching.getEndedBy()).isNull();
        }
        assertThat(matchingRepository.findById(unrelated).orElseThrow().getStatus()).isEqualTo(MatchingStatus.CHATTING);
        assertThat(chatMessageRepository.findAll())
                .allSatisfy(message -> {
                    assertThat(message.getType()).isEqualTo(MessageType.SYSTEM);
                    assertThat(message.getContent()).isEqualTo("상대가 다른 분과 매칭이 완료되었어요.");
                })
                .extracting(message -> message.getMatchingId())
                .containsExactlyInAnyOrder(bookmarkOther, mintOther);
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void confirmedMatchingReturnsCurrentState() {
        matchingConfirmService.confirm(BOOKMARK, matchingId);
        matchingConfirmService.confirm(MINT, matchingId);

        assertThat(matchingConfirmService.confirm(BOOKMARK, matchingId))
                .isEqualTo(new MatchingConfirmResponse(matchingId, MatchingStatus.CONFIRMED, true, true));
    }

    @Test
    void endedMatchingCannotBeConfirmed() {
        setStatus(matchingId, MatchingStatus.ENDED);

        ApiException exception = assertThrows(ApiException.class,
                () -> matchingConfirmService.confirm(BOOKMARK, matchingId));

        assertThat(exception.code()).isEqualTo("INVALID_MATCHING_STATUS");
        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("중지된 매칭은 확정할 수 없습니다.");
    }

    @Test
    void userAlreadyMatchedElsewhereIsRejected() {
        setStatus(start(MINT, DAWN), MatchingStatus.CONFIRMED);

        ApiException exception = assertThrows(ApiException.class,
                () -> matchingConfirmService.confirm(BOOKMARK, matchingId));

        assertThat(exception.code()).isEqualTo("ALREADY_MATCHED");
        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void outsiderAndMissingRoomAreRejected() {
        ApiException forbidden = assertThrows(ApiException.class,
                () -> matchingConfirmService.confirm(OUTSIDER, matchingId));
        ApiException notFound = assertThrows(ApiException.class,
                () -> matchingConfirmService.confirm(BOOKMARK, Long.MAX_VALUE));

        assertThat(forbidden.code()).isEqualTo("MATCHING_FORBIDDEN");
        assertThat(notFound.code()).isEqualTo("MATCHING_NOT_FOUND");
    }

    @Test
    void simultaneousConfirmsBothCount() throws Exception {
        List<Object> outcomes = runTogether(
                () -> matchingConfirmService.confirm(BOOKMARK, matchingId),
                () -> matchingConfirmService.confirm(MINT, matchingId));

        assertThat(outcomes).allMatch(MatchingConfirmResponse.class::isInstance);
        Matching matching = matchingRepository.findById(matchingId).orElseThrow();
        assertThat(matching.getStatus()).isEqualTo(MatchingStatus.CONFIRMED);
        assertThat(matching.isRequesterConfirmed()).isTrue();
        assertThat(matching.isReceiverConfirmed()).isTrue();
    }

    @Test
    void userCannotBeMatchedWithTwoPeopleAtOnce() throws Exception {
        Long withDawn = start(BOOKMARK, DAWN);
        matchingConfirmService.confirm(MINT, matchingId);
        matchingConfirmService.confirm(DAWN, withDawn);

        List<Object> outcomes = runTogether(
                () -> matchingConfirmService.confirm(BOOKMARK, matchingId),
                () -> matchingConfirmService.confirm(BOOKMARK, withDawn));

        assertThat(outcomes).filteredOn(MatchingConfirmResponse.class::isInstance).singleElement()
                .satisfies(outcome -> assertThat(((MatchingConfirmResponse) outcome).status())
                        .isEqualTo(MatchingStatus.CONFIRMED));
        assertThat(outcomes).filteredOn(ApiException.class::isInstance).hasSize(1);
        assertThat(List.of(matchingId, withDawn))
                .extracting(id -> matchingRepository.findById(id).orElseThrow().getStatus())
                .containsExactlyInAnyOrder(MatchingStatus.CONFIRMED, MatchingStatus.ENDED);
    }

    private List<Object> runTogether(Callable<Object> first, Callable<Object> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (Callable<Object> task : List.of(first, second)) {
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        return task.call();
                    } catch (ApiException exception) {
                        return exception;
                    }
                }));
            }
            start.countDown();
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> result : results) {
                outcomes.add(result.get());
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }

    private Long start(long requesterId, long receiverId) {
        return matchingRepository.save(Matching.start(requesterId, receiverId, 1, 1)).getId();
    }

    private void setStatus(Long id, MatchingStatus status) {
        jdbc.sql("update matching set status = ? where matching_id = ?")
                .params(status.name(), id)
                .update();
    }
}
