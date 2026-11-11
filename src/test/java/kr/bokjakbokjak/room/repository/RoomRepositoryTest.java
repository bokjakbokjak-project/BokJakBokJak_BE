package kr.bokjakbokjak.room.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import java.util.Base64;
import java.util.List;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.entity.MessageType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Limit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoomRepositoryTest {
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired EntityManager em;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @Test
    void startedMatchingIsChattingAndFoundInEitherDirection() {
        Matching saved = matchingRepository.saveAndFlush(Matching.start(12L, 7L, 3, 1));

        assertThat(saved.getStatus()).isEqualTo(MatchingStatus.CHATTING);
        assertThat(saved.getUserLowId()).isEqualTo(7L);
        assertThat(saved.getUserHighId()).isEqualTo(12L);
        assertThat(saved.getRequesterSurveyRevision()).isEqualTo(3);
        assertThat(saved.getReceiverSurveyRevision()).isEqualTo(1);
        assertThat(saved.isRequesterConfirmed()).isFalse();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();

        assertThat(matchingRepository.findBetween(7L, 12L)).get().extracting(Matching::getId).isEqualTo(saved.getId());
        assertThat(matchingRepository.findBetween(12L, 7L)).get().extracting(Matching::getId).isEqualTo(saved.getId());
        assertThat(matchingRepository.findBetween(7L, 13L)).isEmpty();
    }

    @Test
    void samePairCannotHaveTwoMatchings() {
        matchingRepository.saveAndFlush(Matching.start(7L, 12L, 1, 1));

        assertThrows(DataIntegrityViolationException.class,
                () -> matchingRepository.saveAndFlush(Matching.start(12L, 7L, 1, 1)));
    }

    @Test
    void findsMatchingsByParticipantAndConfirmedUsers() {
        Matching chatting = matchingRepository.save(Matching.start(7L, 12L, 1, 1));
        Matching confirmed = matchingRepository.save(Matching.start(20L, 7L, 1, 1));
        matchingRepository.save(Matching.start(30L, 31L, 1, 1));
        em.flush();
        setStatus(confirmed.getId(), MatchingStatus.CONFIRMED);

        assertThat(matchingRepository.findAllByParticipant(7L))
                .extracting(Matching::getId).containsExactlyInAnyOrder(chatting.getId(), confirmed.getId());
        assertThat(matchingRepository.findAllByParticipantAndStatusIn(7L, List.of(MatchingStatus.CHATTING)))
                .extracting(Matching::getId).containsExactly(chatting.getId());
        assertThat(matchingRepository.existsConfirmedByParticipant(20L)).isTrue();
        assertThat(matchingRepository.existsConfirmedByParticipant(12L)).isFalse();
        assertThat(matchingRepository.findAllConfirmedByParticipantIn(List.of(12L, 20L, 30L)))
                .extracting(Matching::getId).containsExactly(confirmed.getId());
    }

    @Test
    void agendasAreUniquePerSeqAndScopedToMatching() {
        Long matchingId = matchingRepository.save(Matching.start(7L, 12L, 1, 1)).getId();
        Long otherMatchingId = matchingRepository.save(Matching.start(7L, 13L, 1, 1)).getId();
        Agenda second = agendaRepository.save(
                Agenda.create(matchingId, 2, null, "식기 정리", "언제까지 정리할까요?", AgendaStatus.WAITING));
        Agenda first = agendaRepository.save(
                Agenda.create(matchingId, 1, "BEDTIME", "소등 시간", "몇 시에 불을 끌까요?", AgendaStatus.DISCUSSING));
        agendaRepository.saveAndFlush(
                Agenda.create(otherMatchingId, 1, null, "다른 방", "다른 방 안건", AgendaStatus.DISCUSSING));

        assertThat(agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId))
                .extracting(Agenda::getId).containsExactly(first.getId(), second.getId());
        assertThat(agendaRepository.findByIdAndMatchingId(first.getId(), matchingId)).isPresent();
        assertThat(agendaRepository.findByIdAndMatchingId(first.getId(), otherMatchingId)).isEmpty();

        assertThat(agendaRepository.deleteAllByMatchingId(matchingId)).isEqualTo(2);
        assertThat(agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId)).isEmpty();
        assertThat(agendaRepository.findAllByMatchingIdOrderBySeqAsc(otherMatchingId)).hasSize(1);
    }

    @Test
    void duplicateAgendaSeqIsRejected() {
        Long matchingId = matchingRepository.save(Matching.start(7L, 12L, 1, 1)).getId();
        agendaRepository.saveAndFlush(Agenda.create(matchingId, 1, null, "안건", "내용", AgendaStatus.DISCUSSING));

        assertThrows(DataIntegrityViolationException.class, () -> agendaRepository.saveAndFlush(
                Agenda.create(matchingId, 1, null, "중복", "내용", AgendaStatus.DISCUSSING)));
    }

    @Test
    void messagesArePagedByIdCursor() {
        Long matchingId = matchingRepository.save(Matching.start(7L, 12L, 1, 1)).getId();
        List<ChatMessage> messages = chatMessageRepository.saveAllAndFlush(List.of(
                ChatMessage.ai(matchingId, "첫 번째 안건이에요.", null),
                ChatMessage.user(matchingId, 7L, "안녕하세요", null),
                ChatMessage.user(matchingId, 12L, "반가워요", null),
                ChatMessage.system(matchingId, "책갈피님이 안건을 확정했어요.", null),
                ChatMessage.user(matchingId, 7L, "좋아요", null)));
        List<Long> ids = messages.stream().map(ChatMessage::getId).toList();

        assertThat(chatMessageRepository.findAllByMatchingIdAndIdGreaterThanOrderByIdAsc(
                matchingId, ids.get(1), Limit.of(2)))
                .extracting(ChatMessage::getId).containsExactly(ids.get(2), ids.get(3));
        assertThat(chatMessageRepository.findAllByMatchingIdAndIdLessThanOrderByIdDesc(
                matchingId, ids.get(4), Limit.of(2)))
                .extracting(ChatMessage::getId).containsExactly(ids.get(3), ids.get(2));
        assertThat(chatMessageRepository.findAllByMatchingIdOrderByIdDesc(matchingId, Limit.of(1)))
                .extracting(ChatMessage::getId).containsExactly(ids.get(4));

        ChatMessage ai = messages.getFirst();
        assertThat(ai.getType()).isEqualTo(MessageType.AI);
        assertThat(ai.getSenderId()).isNull();
        assertThat(ai.getCreatedAt()).isNotNull();
    }

    @Test
    void latestMessagePerMatching() {
        Long first = matchingRepository.save(Matching.start(7L, 12L, 1, 1)).getId();
        Long second = matchingRepository.save(Matching.start(7L, 13L, 1, 1)).getId();
        Long empty = matchingRepository.save(Matching.start(7L, 14L, 1, 1)).getId();
        chatMessageRepository.save(ChatMessage.user(first, 7L, "a", null));
        ChatMessage firstLatest = chatMessageRepository.save(ChatMessage.user(first, 12L, "b", null));
        ChatMessage secondLatest = chatMessageRepository.saveAndFlush(ChatMessage.user(second, 13L, "c", null));

        assertThat(chatMessageRepository.findLatestByMatchingIdIn(List.of(first, second, empty)))
                .extracting(ChatMessage::getId)
                .containsExactlyInAnyOrder(firstLatest.getId(), secondLatest.getId());
    }

    private void setStatus(Long matchingId, MatchingStatus status) {
        em.createQuery("update Matching m set m.status = :status where m.id = :id")
                .setParameter("status", status)
                .setParameter("id", matchingId)
                .executeUpdate();
        em.clear();
    }
}
