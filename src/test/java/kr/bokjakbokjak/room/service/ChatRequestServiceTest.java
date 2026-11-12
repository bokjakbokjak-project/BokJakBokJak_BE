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
import kr.bokjakbokjak.notification.entity.Notification;
import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import kr.bokjakbokjak.room.dto.ChatRequestResponse;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.event.ChatStartedEvent;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class ChatRequestServiceTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final long OTHER = 13L;

    @Autowired ChatRequestService chatRequestService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JdbcClient jdbc;
    @Autowired ApplicationContext context;
    @Autowired ApplicationEvents events;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        users.addUser(BOOKMARK, "책갈피", 1);
        users.addUser(MINT, "민트초코", 3);
        users.addUser(OTHER, "새벽감성", 2);
        users.connect(BOOKMARK, MINT);
        users.connect(BOOKMARK, OTHER);
        users.connect(MINT, OTHER);
    }

    @AfterEach
    void cleanUp() {
        notificationRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void startsChattingWithRevisionsNotificationAndEvent() {
        ChatRequestResponse response = chatRequestService.request(BOOKMARK, MINT);

        assertThat(response.status()).isEqualTo(MatchingStatus.CHATTING);
        assertThat(response.createdAt()).isNotNull();
        Matching saved = matchingRepository.findById(response.matchingId()).orElseThrow();
        assertThat(saved.getRequesterId()).isEqualTo(BOOKMARK);
        assertThat(saved.getReceiverId()).isEqualTo(MINT);
        assertThat(saved.getRequesterSurveyRevision()).isEqualTo(1);
        assertThat(saved.getReceiverSurveyRevision()).isEqualTo(3);

        assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getReceiverId()).isEqualTo(MINT);
            assertThat(notification.getMatchingId()).isEqualTo(response.matchingId());
            assertThat(notification.getType()).isEqualTo(NotificationType.CHAT_REQUESTED);
            assertThat(notification.getContent()).isEqualTo("책갈피님과의 매칭이 시작되었어요");
        });
        assertThat(events.stream(ChatStartedEvent.class))
                .containsExactly(new ChatStartedEvent(response.matchingId()));
    }

    @Test
    void agendaPreparationIsOffInTests() {
        assertThat(context.getBeanNamesForType(AgendaPreparationListener.class)).isEmpty();
    }

    @Test
    void requesterMustCompleteSurvey() {
        users.addUser(BOOKMARK, "책갈피", 0);

        assertRejected(() -> chatRequestService.request(BOOKMARK, MINT), "ONBOARDING_REQUIRED", HttpStatus.CONFLICT);
    }

    @Test
    void cannotRequestSelf() {
        assertRejected(() -> chatRequestService.request(BOOKMARK, BOOKMARK), "CANNOT_REQUEST_SELF",
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void withdrawnOrMissingReceiverIsRejected() {
        users.withdraw(MINT);

        assertRejected(() -> chatRequestService.request(BOOKMARK, MINT), "TARGET_WITHDRAWN", HttpStatus.NOT_FOUND);
        assertRejected(() -> chatRequestService.request(BOOKMARK, 404L), "TARGET_WITHDRAWN", HttpStatus.NOT_FOUND);
    }

    @Test
    void receiverMustBeCandidate() {
        users.addUser(20L, "다른학교", 1);

        assertRejected(() -> chatRequestService.request(BOOKMARK, 20L), "TARGET_NOT_ELIGIBLE", HttpStatus.FORBIDDEN);
    }

    @Test
    void existingChatInEitherDirectionReturnsItsId() {
        Long matchingId = chatRequestService.request(MINT, BOOKMARK).matchingId();

        ApiException exception = assertThrows(ApiException.class, () -> chatRequestService.request(BOOKMARK, MINT));

        assertThat(exception.code()).isEqualTo("ALREADY_CHATTING");
        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.errors()).containsEntry("matching_id", String.valueOf(matchingId));
        assertThat(matchingRepository.count()).isEqualTo(1);
    }

    @Test
    void endedPairCannotRequestAgain() {
        Long matchingId = chatRequestService.request(BOOKMARK, MINT).matchingId();
        setStatus(matchingId, MatchingStatus.ENDED);

        assertRejected(() -> chatRequestService.request(MINT, BOOKMARK), "MATCHING_ENDED_PAIR", HttpStatus.CONFLICT);
    }

    @Test
    void eitherUserAlreadyMatchedIsRejected() {
        Long matchingId = chatRequestService.request(MINT, OTHER).matchingId();
        setStatus(matchingId, MatchingStatus.CONFIRMED);

        assertRejected(() -> chatRequestService.request(BOOKMARK, MINT), "ALREADY_MATCHED", HttpStatus.CONFLICT);
    }

    @Test
    void simultaneousRequestsCreateOneRoom() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (long[] pair : new long[][] {{BOOKMARK, MINT}, {MINT, BOOKMARK}}) {
                Callable<Object> call = () -> {
                    start.await();
                    try {
                        return chatRequestService.request(pair[0], pair[1]);
                    } catch (ApiException exception) {
                        return exception;
                    }
                };
                results.add(executor.submit(call));
            }
            start.countDown();

            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> result : results) {
                outcomes.add(result.get());
            }

            assertThat(matchingRepository.count()).isEqualTo(1);
            assertThat(outcomes).filteredOn(ChatRequestResponse.class::isInstance).hasSize(1);
            assertThat(outcomes).filteredOn(ApiException.class::isInstance).singleElement()
                    .satisfies(outcome -> assertThat(((ApiException) outcome).code()).isEqualTo("ALREADY_CHATTING"));
            assertThat(notificationRepository.findAll()).extracting(Notification::getReceiverId).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertRejected(Executable executable, String code, HttpStatus status) {
        ApiException exception = assertThrows(ApiException.class, executable);
        assertThat(exception.code()).isEqualTo(code);
        assertThat(exception.status()).isEqualTo(status);
    }

    private void setStatus(Long matchingId, MatchingStatus status) {
        jdbc.sql("update matching set status = ? where matching_id = ?")
                .params(status.name(), matchingId)
                .update();
    }
}
