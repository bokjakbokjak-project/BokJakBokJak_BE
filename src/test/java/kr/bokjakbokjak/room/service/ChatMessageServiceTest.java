package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import java.util.Base64;
import java.util.List;
import java.util.stream.IntStream;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.room.dto.MessageResponse;
import kr.bokjakbokjak.room.dto.PreviousMessagesResponse;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.entity.MessageType;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
class ChatMessageServiceTest {
    private static final Long REQUESTER = 7L;
    private static final Long RECEIVER = 12L;
    private static final Long OUTSIDER = 99L;

    @Autowired ChatMessageService chatMessageService;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired EntityManager em;

    private Long matchingId;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        matchingId = matchingRepository.saveAndFlush(Matching.start(REQUESTER, RECEIVER, 1, 1)).getId();
    }

    @Test
    void newMessagesReturnsOnlyMessagesAfterCursor() {
        List<Long> ids = saveMessages(4);

        assertThat(chatMessageService.getNewMessages(RECEIVER, matchingId, ids.get(1), 50).messages())
                .extracting(MessageResponse::messageId).containsExactly(ids.get(2), ids.get(3));
        assertThat(chatMessageService.getNewMessages(RECEIVER, matchingId, ids.get(0), 2).messages())
                .extracting(MessageResponse::messageId).containsExactly(ids.get(1), ids.get(2));
        assertThat(chatMessageService.getNewMessages(RECEIVER, matchingId, ids.get(3), 50).messages()).isEmpty();
    }

    @Test
    void previousMessagesWithoutCursorReturnsLatestInAscendingOrder() {
        List<Long> ids = saveMessages(5);

        PreviousMessagesResponse response = chatMessageService.getPreviousMessages(REQUESTER, matchingId, null, 3);

        assertThat(response.messages()).extracting(MessageResponse::messageId)
                .containsExactly(ids.get(2), ids.get(3), ids.get(4));
        assertThat(response.hasMore()).isTrue();
    }

    @Test
    void previousMessagesBeforeCursorReportsWhetherMoreRemain() {
        List<Long> ids = saveMessages(5);

        PreviousMessagesResponse middle = chatMessageService.getPreviousMessages(REQUESTER, matchingId, ids.get(3), 2);
        PreviousMessagesResponse oldest = chatMessageService.getPreviousMessages(REQUESTER, matchingId, ids.get(1), 2);

        assertThat(middle.messages()).extracting(MessageResponse::messageId).containsExactly(ids.get(1), ids.get(2));
        assertThat(middle.hasMore()).isTrue();
        assertThat(oldest.messages()).extracting(MessageResponse::messageId).containsExactly(ids.get(0));
        assertThat(oldest.hasMore()).isFalse();
    }

    @Test
    void endedRoomHistoryCanStillBeRead() {
        List<Long> ids = saveMessages(1);
        setStatus(MatchingStatus.ENDED);

        assertThat(chatMessageService.getPreviousMessages(REQUESTER, matchingId, null, 30).messages())
                .extracting(MessageResponse::messageId).containsExactly(ids.getFirst());
    }

    @Test
    void sendStoresUserMessageWithDiscussingAgenda() {
        agendaRepository.save(Agenda.create(matchingId, 2, null, "식기", "식기는 언제 정리할까요?", AgendaStatus.WAITING));
        Long discussing = agendaRepository.saveAndFlush(
                Agenda.create(matchingId, 1, "BEDTIME", "소등", "몇 시에 불을 끌까요?", AgendaStatus.DISCUSSING)).getId();

        MessageResponse sent = chatMessageService.send(REQUESTER, matchingId, "저는 12시쯤 자요.");

        assertThat(sent.type()).isEqualTo(MessageType.USER);
        assertThat(sent.senderId()).isEqualTo(REQUESTER);
        assertThat(sent.content()).isEqualTo("저는 12시쯤 자요.");
        assertThat(sent.agendaId()).isEqualTo(discussing);
        assertThat(sent.createdAt()).isNotNull();
        assertThat(chatMessageRepository.findById(sent.messageId())).isPresent();
    }

    @Test
    void sendWithoutAgendaStoresNullAgenda() {
        assertThat(chatMessageService.send(RECEIVER, matchingId, "안녕하세요").agendaId()).isNull();
    }

    @Test
    void cannotSendToEndedOrConfirmedRoom() {
        setStatus(MatchingStatus.ENDED);
        assertInvalidStatus(() -> chatMessageService.send(REQUESTER, matchingId, "안녕하세요"));

        setStatus(MatchingStatus.CONFIRMED);
        assertInvalidStatus(() -> chatMessageService.send(REQUESTER, matchingId, "안녕하세요"));
    }

    @Test
    void outsiderAndMissingRoomAreRejected() {
        ApiException forbidden = assertThrows(ApiException.class,
                () -> chatMessageService.getNewMessages(OUTSIDER, matchingId, 0, 50));
        ApiException notFound = assertThrows(ApiException.class,
                () -> chatMessageService.send(REQUESTER, Long.MAX_VALUE, "안녕하세요"));

        assertThat(forbidden.code()).isEqualTo("MATCHING_FORBIDDEN");
        assertThat(forbidden.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notFound.code()).isEqualTo("MATCHING_NOT_FOUND");
        assertThat(notFound.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void assertInvalidStatus(Executable executable) {
        ApiException exception = assertThrows(ApiException.class, executable);
        assertThat(exception.code()).isEqualTo("INVALID_MATCHING_STATUS");
        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("조율 중인 채팅방에만 메시지를 보낼 수 있습니다.");
    }

    private List<Long> saveMessages(int count) {
        List<ChatMessage> messages = IntStream.range(0, count)
                .mapToObj(i -> ChatMessage.user(matchingId, REQUESTER, "메시지 " + i, null))
                .toList();
        return chatMessageRepository.saveAllAndFlush(messages).stream().map(ChatMessage::getId).toList();
    }

    private void setStatus(MatchingStatus status) {
        em.createQuery("update Matching m set m.status = :status where m.id = :id")
                .setParameter("status", status)
                .setParameter("id", matchingId)
                .executeUpdate();
        em.clear();
    }
}
