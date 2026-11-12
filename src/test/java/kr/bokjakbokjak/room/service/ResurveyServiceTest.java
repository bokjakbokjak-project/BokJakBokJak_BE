package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import kr.bokjakbokjak.common.SurveyRevisionChangedEvent;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
import kr.bokjakbokjak.room.dto.ChatRoomResponse;
import kr.bokjakbokjak.room.dto.MatchingConfirmResponse;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.entity.MessageType;
import kr.bokjakbokjak.room.event.AgendasResetEvent;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
class ResurveyServiceTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final long DAWN = 13L;

    @Autowired ResurveyService resurveyService;
    @Autowired ChatRoomService chatRoomService;
    @Autowired MatchingConfirmService matchingConfirmService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired TransactionTemplate transactionTemplate;
    @Autowired ApplicationEvents events;
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
        matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();
        agendaRepository.save(Agenda.create(matchingId, 1, "BEDTIME", "평일 소등 시간",
                "평일에는 몇 시에 방 불을 끌까요?", AgendaStatus.DECIDED));
        agendaRepository.save(Agenda.create(matchingId, 2, "DISHES", "식기 정리",
                "사용한 컵이나 그릇은 언제까지 정리할까요?", AgendaStatus.DISCUSSING));
        chatMessageRepository.save(ChatMessage.user(matchingId, MINT, "안녕하세요", null));
        jdbc.sql("update matching set receiver_confirmed = true where matching_id = ?").param(matchingId).update();
    }

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAll();
        agendaRepository.deleteAll();
        notificationRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void resurveyResetsChattingRoomButKeepsHistory() {
        int reset = resurveyService.resetChattingRooms(BOOKMARK, 2);

        assertThat(reset).isEqualTo(1);
        Matching matching = matchingRepository.findById(matchingId).orElseThrow();
        assertThat(matching.getRequesterSurveyRevision()).isEqualTo(2);
        assertThat(matching.isRequesterConfirmed()).isFalse();
        assertThat(matching.isReceiverConfirmed()).isFalse();
        assertThat(agendaRepository.existsByMatchingId(matchingId)).isFalse();
        assertThat(messages()).extracting(ChatMessage::getType, ChatMessage::getContent).containsExactly(
                tuple(MessageType.USER, "안녕하세요"),
                tuple(MessageType.SYSTEM, "책갈피님이 설문을 수정해서 안건을 새로 준비할게요."));
        assertThat(events.stream(AgendasResetEvent.class)).containsExactly(new AgendasResetEvent(matchingId));
    }

    @Test
    void sameRevisionIsResetOnlyOnce() {
        resurveyService.resetChattingRooms(BOOKMARK, 2);

        assertThat(resurveyService.resetChattingRooms(BOOKMARK, 2)).isZero();
        assertThat(messages()).hasSize(2);
    }

    @Test
    void endedAndConfirmedRoomsAreLeftAlone() {
        Long ended = matchingRepository.save(Matching.start(BOOKMARK, DAWN, 1, 1)).getId();
        jdbc.sql("update matching set status = ? where matching_id = ?")
                .params(MatchingStatus.ENDED.name(), ended).update();
        jdbc.sql("update matching set status = ? where matching_id = ?")
                .params(MatchingStatus.CONFIRMED.name(), matchingId).update();

        assertThat(resurveyService.resetChattingRooms(BOOKMARK, 2)).isZero();
        assertThat(agendaRepository.existsByMatchingId(matchingId)).isTrue();
        assertThat(matchingRepository.findById(ended).orElseThrow().getRequesterSurveyRevision()).isEqualTo(1);
    }

    @Test
    void surveyChangeEventResetsRoomsAfterCommit() throws InterruptedException {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(new SurveyRevisionChangedEvent(MINT, 2)));

        long deadline = System.currentTimeMillis() + 5_000;
        while (agendaRepository.existsByMatchingId(matchingId) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(matchingRepository.findById(matchingId).orElseThrow().getReceiverSurveyRevision()).isEqualTo(2);
        assertThat(messages()).extracting(ChatMessage::getContent)
                .contains("민트초코님이 설문을 수정해서 안건을 새로 준비할게요.");
    }

    @Test
    void roomInfoCatchesMissedResurvey() {
        users.addUser(MINT, "민트초코", 2);

        ChatRoomResponse room = chatRoomService.getRoom(BOOKMARK, matchingId);

        assertThat(room.agendas()).isEmpty();
        assertThat(room.partnerConfirmed()).isFalse();
        assertThat(messages()).extracting(ChatMessage::getType).containsExactly(MessageType.USER, MessageType.SYSTEM);
    }

    @Test
    void confirmAfterResurveyKeepsOnlyNewChoice() {
        users.addUser(MINT, "민트초코", 2);

        MatchingConfirmResponse response = matchingConfirmService.confirm(BOOKMARK, matchingId);

        assertThat(response).isEqualTo(new MatchingConfirmResponse(matchingId, MatchingStatus.CHATTING, true, false));
    }

    @Test
    void simultaneousResetsLeaveOneSystemMessage() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(executor.submit(() -> {
                    start.await();
                    return resurveyService.resetChattingRooms(BOOKMARK, 2);
                }));
            }
            start.countDown();
            int total = 0;
            for (Future<Integer> result : results) {
                total += result.get();
            }

            assertThat(total).isEqualTo(1);
            assertThat(messages()).filteredOn(message -> message.getType() == MessageType.SYSTEM).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private List<ChatMessage> messages() {
        return chatMessageRepository.findAll().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();
    }
}
