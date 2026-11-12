package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.notification.repository.NotificationRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class AgendaPreparationTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final String AI_RESPONSE = """
            {"agendas":[{"topic_code":"ALARM","title":"아침 알람","content":"아침 알람은 몇 번까지 울리게 할까요?"},\
            {"topic_code":"BEDTIME","title":"평일 소등 시간","content":"평일에는 몇 시에 방 불을 끌까요?"}]}""";

    @MockitoBean MatchingEngine matchingEngine;
    @MockitoBean LlmClient llmClient;

    @Autowired AgendaPreparationListener listener;
    @Autowired ChatRequestService chatRequestService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired JdbcClient jdbc;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        given(matchingEngine.calculate(anyLong(), anyLong())).willAnswer(invocation -> new MatchResultDto(
                invocation.getArgument(0), invocation.getArgument(1),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, AgendaGeneratorTest.items()));
        given(llmClient.complete(any())).willReturn(Optional.of(AI_RESPONSE));
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
    void savesTwoAgendasAndIntroducesTheFirst() {
        Long matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();

        listener.prepare(matchingId);

        assertThat(agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId))
                .extracting(Agenda::getSeq, Agenda::getTopicCode, Agenda::getTitle, Agenda::getStatus)
                .containsExactly(
                        tuple(1, "ALARM", "아침 알람", AgendaStatus.DISCUSSING),
                        tuple(2,"BEDTIME", "평일 소등 시간", AgendaStatus.WAITING));
        Agenda first = agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId).getFirst();
        assertThat(chatMessageRepository.findAll()).singleElement().satisfies(message -> {
            assertThat(message.getType()).isEqualTo(MessageType.AI);
            assertThat(message.getSenderId()).isNull();
            assertThat(message.getAgendaId()).isEqualTo(first.getId());
            assertThat(message.getContent()).isEqualTo("첫 번째 안건이에요. 아침 알람은 몇 번까지 울리게 할까요?");
        });
    }

    @Test
    void doesNotCreateAgendasTwice() {
        Long matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();

        listener.prepare(matchingId);
        listener.prepare(matchingId);

        assertThat(agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId)).hasSize(2);
        assertThat(chatMessageRepository.count()).isEqualTo(1);
    }

    @Test
    void skipsRoomThatIsNoLongerChatting() {
        Long matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();
        jdbc.sql("update matching set status = ? where matching_id = ?")
                .params(MatchingStatus.ENDED.name(), matchingId)
                .update();

        listener.prepare(matchingId);

        assertThat(agendaRepository.existsByMatchingId(matchingId)).isFalse();
        assertThat(chatMessageRepository.count()).isZero();
    }

    @Test
    void fallsBackToDefaultAgendasWhenAiIsUnavailable() {
        given(llmClient.complete(any())).willReturn(Optional.empty());
        Long matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();

        listener.prepare(matchingId);

        assertThat(agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId))
                .extracting(Agenda::getTitle).containsExactly("아침 알람", "평일 소등 시간");
    }

    @Test
    void chatRequestPreparesAgendasInBackground() throws InterruptedException {
        users.addUser(BOOKMARK, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        users.connect(BOOKMARK, MINT);

        Long matchingId = chatRequestService.request(BOOKMARK, MINT).matchingId();

        long deadline = System.currentTimeMillis() + 5_000;
        while (!agendaRepository.existsByMatchingId(matchingId) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId)).hasSize(2);
        assertThat(chatMessageRepository.findAll()).extracting(ChatMessage::getType).containsExactly(MessageType.AI);
    }
}
