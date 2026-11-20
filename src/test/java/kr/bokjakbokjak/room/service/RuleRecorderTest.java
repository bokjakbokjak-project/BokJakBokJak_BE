package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Base64;
import java.util.Optional;
import kr.bokjakbokjak.ai.LlmClient;
import kr.bokjakbokjak.ai.LlmRequest;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class RuleRecorderTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final String RULE = "{\"rule\":\"평일에는 밤 12시에 불을 끄기로 했어요.\"}";

    @MockitoBean LlmClient llmClient;
    @Autowired RuleRecorder ruleRecorder;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired ApplicationContext context;

    private Long matchingId;
    private Long agendaId;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();
        agendaId = agendaRepository.save(Agenda.create(matchingId, 1, "BEDTIME", "평일 소등 시간",
                "평일에는 몇 시에 방 불을 끌까요?", AgendaStatus.DECIDED)).getId();
        given(llmClient.complete(any())).willReturn(Optional.of(RULE));
    }

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAll();
        agendaRepository.deleteAll();
        matchingRepository.deleteAll();
    }

    @Test
    void recordsRuleFromThisAgendaChatOnly() {
        chatMessageRepository.save(ChatMessage.user(matchingId, BOOKMARK, "평일엔 12시에 끌까요?", agendaId));
        chatMessageRepository.save(ChatMessage.ai(matchingId, "첫 번째 안건이에요.", agendaId));
        chatMessageRepository.save(ChatMessage.user(matchingId, MINT, "좋아요", agendaId));
        chatMessageRepository.save(ChatMessage.user(matchingId, MINT, "다른 안건 이야기", null));

        ruleRecorder.record(matchingId, agendaId);

        assertThat(agendaRepository.findById(agendaId).orElseThrow().getRuleSummary())
                .isEqualTo("평일에는 밤 12시에 불을 끄기로 했어요.");
        ArgumentCaptor<LlmRequest> request = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmClient).complete(request.capture());
        assertThat(request.getValue().userPrompt()).endsWith("""
                대화
                A: 평일엔 12시에 끌까요?
                B: 좋아요""");
    }

    @Test
    void agendaWithoutChatStaysEmpty() {
        ruleRecorder.record(matchingId, agendaId);

        assertThat(agendaRepository.findById(agendaId).orElseThrow().getRuleSummary()).isNull();
        verify(llmClient, never()).complete(any());
    }

    @Test
    void deletedAgendaIsSkipped() {
        chatMessageRepository.save(ChatMessage.user(matchingId, BOOKMARK, "12시에 끌까요?", agendaId));
        agendaRepository.deleteById(agendaId);

        ruleRecorder.record(matchingId, agendaId);

        verify(llmClient, never()).complete(any());
    }

    @Test
    void autoSummaryIsOffInTests() {
        assertThat(context.getBeanNamesForType(RuleSummaryListener.class)).isEmpty();
    }
}
