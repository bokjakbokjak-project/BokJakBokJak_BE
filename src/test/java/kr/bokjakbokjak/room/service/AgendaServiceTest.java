package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
import kr.bokjakbokjak.room.dto.AgendaDecideResponse;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.entity.MessageType;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class AgendaServiceTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final long OUTSIDER = 99L;

    @Autowired AgendaService agendaService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired JdbcClient jdbc;

    private Long matchingId;
    private Long firstId;
    private Long secondId;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        users.addUser(BOOKMARK, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();
        firstId = agendaRepository.save(Agenda.create(matchingId, 1, "BEDTIME", "평일 소등 시간",
                "평일에는 몇 시에 방 불을 끌까요?", AgendaStatus.DISCUSSING)).getId();
        secondId = agendaRepository.save(Agenda.create(matchingId, 2, "DISHES", "식기 정리",
                "사용한 컵이나 그릇은 언제까지 정리할까요?", AgendaStatus.WAITING)).getId();
    }

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAll();
        agendaRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void decidingStartsNextAgendaWithMessages() {
        AgendaDecideResponse response = agendaService.decide(MINT, matchingId, firstId);

        assertThat(response).isEqualTo(new AgendaDecideResponse(firstId, AgendaStatus.DECIDED, MINT, secondId, false));
        Agenda first = agendaRepository.findById(firstId).orElseThrow();
        assertThat(first.getDecidedAt()).isNotNull();
        assertThat(agendaRepository.findById(secondId).orElseThrow().getStatus()).isEqualTo(AgendaStatus.DISCUSSING);
        assertThat(messages()).extracting(ChatMessage::getType, ChatMessage::getAgendaId, ChatMessage::getContent)
                .containsExactly(
                        tuple(MessageType.SYSTEM, firstId, "민트초코님이 안건을 확정했어요."),
                        tuple(MessageType.AI, secondId,
                                "두 번째 안건이에요. 사용한 컵이나 그릇은 언제까지 정리할까요?"));
    }

    @Test
    void decidingLastAgendaFinishesAll() {
        agendaService.decide(BOOKMARK, matchingId, firstId);

        AgendaDecideResponse response = agendaService.decide(MINT, matchingId, secondId);

        assertThat(response).isEqualTo(new AgendaDecideResponse(secondId, AgendaStatus.DECIDED, MINT, null, true));
        assertThat(messages()).extracting(ChatMessage::getType)
                .containsExactly(MessageType.SYSTEM, MessageType.AI, MessageType.SYSTEM);
    }

    @Test
    void decidedAgendaReturnsCurrentStateWithoutNewMessages() {
        agendaService.decide(BOOKMARK, matchingId, firstId);

        AgendaDecideResponse again = agendaService.decide(MINT, matchingId, firstId);

        assertThat(again).isEqualTo(new AgendaDecideResponse(firstId, AgendaStatus.DECIDED, BOOKMARK, secondId, false));
        assertThat(messages()).hasSize(2);
    }

    @Test
    void waitingAgendaCannotBeDecided() {
        assertRejected(() -> agendaService.decide(BOOKMARK, matchingId, secondId),
                "AGENDA_NOT_STARTED", HttpStatus.CONFLICT);
    }

    @Test
    void agendaOfAnotherRoomIsNotFound() {
        Long otherMatching = matchingRepository.save(Matching.start(BOOKMARK, 13L, 1, 1)).getId();

        assertRejected(() -> agendaService.decide(BOOKMARK, otherMatching, firstId), "AGENDA_NOT_FOUND",
                HttpStatus.NOT_FOUND);
        assertRejected(() -> agendaService.decide(BOOKMARK, matchingId, Long.MAX_VALUE), "AGENDA_NOT_FOUND",
                HttpStatus.NOT_FOUND);
    }

    @Test
    void outsiderAndMissingRoomAreRejected() {
        assertRejected(() -> agendaService.decide(OUTSIDER, matchingId, firstId), "MATCHING_FORBIDDEN",
                HttpStatus.FORBIDDEN);
        assertRejected(() -> agendaService.decide(BOOKMARK, Long.MAX_VALUE, firstId), "MATCHING_NOT_FOUND",
                HttpStatus.NOT_FOUND);
    }

    @Test
    void roomMustBeChatting() {
        jdbc.sql("update matching set status = ? where matching_id = ?")
                .params(MatchingStatus.ENDED.name(), matchingId)
                .update();

        ApiException exception = assertThrows(ApiException.class,
                () -> agendaService.decide(BOOKMARK, matchingId, firstId));

        assertThat(exception.code()).isEqualTo("INVALID_MATCHING_STATUS");
        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("조율 중인 채팅방에서만 안건을 확정할 수 있습니다.");
    }

    @Test
    void simultaneousDecisionsLeaveOneSystemMessage() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<AgendaDecideResponse>> results = new ArrayList<>();
            for (long userId : new long[] {BOOKMARK, MINT}) {
                Callable<AgendaDecideResponse> call = () -> {
                    start.await();
                    return agendaService.decide(userId, matchingId, firstId);
                };
                results.add(executor.submit(call));
            }
            start.countDown();

            List<AgendaDecideResponse> responses = new ArrayList<>();
            for (Future<AgendaDecideResponse> result : results) {
                responses.add(result.get());
            }

            assertThat(responses).extracting(AgendaDecideResponse::status).containsOnly(AgendaStatus.DECIDED);
            assertThat(responses).extracting(AgendaDecideResponse::nextAgendaId).containsOnly(secondId);
            assertThat(messages()).extracting(ChatMessage::getType)
                    .containsExactly(MessageType.SYSTEM, MessageType.AI);
        } finally {
            executor.shutdownNow();
        }
    }

    private List<ChatMessage> messages() {
        return chatMessageRepository.findAll().stream()
                .sorted((left, right) -> left.getId().compareTo(right.getId()))
                .toList();
    }

    private void assertRejected(Executable executable, String code, HttpStatus status) {
        ApiException exception = assertThrows(ApiException.class, executable);
        assertThat(exception.code()).isEqualTo(code);
        assertThat(exception.status()).isEqualTo(status);
    }
}
