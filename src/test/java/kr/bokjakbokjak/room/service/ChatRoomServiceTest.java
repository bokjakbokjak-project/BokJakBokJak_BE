package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.room.dto.ChatRoomListResponse;
import kr.bokjakbokjak.room.dto.ChatRoomResponse;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.support.FakeUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class ChatRoomServiceTest {
    private static final Long ME = 7L;
    private static final Long MINT = 12L;
    private static final Long CLOVER = 13L;
    private static final Long UNKNOWN = 14L;
    private static final Long OUTSIDER = 99L;

    @Autowired ChatRoomService chatRoomService;
    @Autowired MatchingRepository matchingRepository;
    @Autowired AgendaRepository agendaRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
    @Autowired FakeUserQueryService users;
    @Autowired EntityManager em;

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        users.addUser(ME, "책갈피", 1);
        users.addUser(MINT, "민트초코", 1);
        users.addUser(CLOVER, "네잎클로버", 1);
    }

    @AfterEach
    void cleanUp() {
        users.clear();
    }

    @Test
    void myRoomsAreEmptyWithoutMatchings() {
        assertThat(chatRoomService.getMyRooms(ME).rooms()).isEmpty();
    }

    @Test
    void myRoomsExcludeEndedAndAreOrderedByRecentActivity() {
        Long older = startMatching(ME, MINT, "2026-11-01T00:00:00Z");
        Long newer = startMatching(CLOVER, ME, "2026-11-02T00:00:00Z");
        Long ended = startMatching(ME, UNKNOWN, "2026-11-03T00:00:00Z");
        setStatus(ended, MatchingStatus.ENDED);
        saveMessage(older, "저는 12시쯤 자요.", "2026-11-04T00:00:00Z");

        List<ChatRoomListResponse.Room> rooms = chatRoomService.getMyRooms(ME).rooms();

        assertThat(rooms).extracting(ChatRoomListResponse.Room::matchingId).containsExactly(older, newer);
        assertThat(rooms.get(0).partner().nickname()).isEqualTo("민트초코");
        assertThat(rooms.get(0).lastMessage()).isEqualTo("저는 12시쯤 자요.");
        assertThat(rooms.get(0).lastMessageAt()).isEqualTo(Instant.parse("2026-11-04T00:00:00Z"));
        assertThat(rooms.get(1).partner().userId()).isEqualTo(CLOVER);
        assertThat(rooms.get(1).lastMessage()).isNull();
        assertThat(rooms.get(1).lastMessageAt()).isNull();
    }

    @Test
    void confirmedRoomIsListed() {
        Long confirmed = startMatching(ME, MINT, "2026-11-01T00:00:00Z");
        setStatus(confirmed, MatchingStatus.CONFIRMED);

        assertThat(chatRoomService.getMyRooms(ME).rooms())
                .extracting(ChatRoomListResponse.Room::status).containsExactly(MatchingStatus.CONFIRMED);
    }

    @Test
    void missingPartnerIsShownAsUnknownUser() {
        startMatching(ME, UNKNOWN, "2026-11-01T00:00:00Z");

        ChatRoomListResponse.Partner partner = chatRoomService.getMyRooms(ME).rooms().getFirst().partner();

        assertThat(partner.userId()).isEqualTo(UNKNOWN);
        assertThat(partner.nickname()).isEqualTo("알 수 없는 사용자");
        assertThat(partner.topCategories()).isEmpty();
    }

    @Test
    void roomShowsPartnerAndAgendasInOrder() {
        Long matchingId = startMatching(ME, MINT, "2026-11-01T00:00:00Z");
        agendaRepository.save(Agenda.create(matchingId, 2, null, "식기", "식기는 언제 정리할까요?", AgendaStatus.WAITING));
        agendaRepository.saveAndFlush(
                Agenda.create(matchingId, 1, "BEDTIME", "소등", "몇 시에 불을 끌까요?", AgendaStatus.DISCUSSING));

        ChatRoomResponse room = chatRoomService.getRoom(ME, matchingId);

        assertThat(room.matchingId()).isEqualTo(matchingId);
        assertThat(room.status()).isEqualTo(MatchingStatus.CHATTING);
        assertThat(room.endReason()).isNull();
        assertThat(room.partner().userId()).isEqualTo(MINT);
        assertThat(room.partner().nickname()).isEqualTo("민트초코");
        assertThat(room.partner().department()).isEqualTo("컴퓨터공학과");
        assertThat(room.agendas()).extracting(ChatRoomResponse.AgendaItem::seq).containsExactly(1, 2);
        assertThat(room.agendas().getFirst().status()).isEqualTo(AgendaStatus.DISCUSSING);
    }

    @Test
    void roomWithoutAgendasReturnsEmptyList() {
        Long matchingId = startMatching(ME, MINT, "2026-11-01T00:00:00Z");

        assertThat(chatRoomService.getRoom(ME, matchingId).agendas()).isEmpty();
    }

    @Test
    void confirmedFlagsAreRelativeToViewer() {
        Long matchingId = startMatching(ME, MINT, "2026-11-01T00:00:00Z");
        em.createQuery("update Matching m set m.receiverConfirmed = true where m.id = :id")
                .setParameter("id", matchingId)
                .executeUpdate();
        em.clear();

        ChatRoomResponse requesterView = chatRoomService.getRoom(ME, matchingId);
        ChatRoomResponse receiverView = chatRoomService.getRoom(MINT, matchingId);

        assertThat(requesterView.myConfirmed()).isFalse();
        assertThat(requesterView.partnerConfirmed()).isTrue();
        assertThat(receiverView.myConfirmed()).isTrue();
        assertThat(receiverView.partnerConfirmed()).isFalse();
    }

    @Test
    void outsiderAndMissingRoomAreRejected() {
        Long matchingId = startMatching(ME, MINT, "2026-11-01T00:00:00Z");

        ApiException forbidden = assertThrows(ApiException.class, () -> chatRoomService.getRoom(OUTSIDER, matchingId));
        ApiException notFound = assertThrows(ApiException.class, () -> chatRoomService.getRoom(ME, Long.MAX_VALUE));

        assertThat(forbidden.code()).isEqualTo("MATCHING_FORBIDDEN");
        assertThat(forbidden.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(notFound.code()).isEqualTo("MATCHING_NOT_FOUND");
        assertThat(notFound.status()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private Long startMatching(Long requesterId, Long receiverId, String createdAt) {
        Long id = matchingRepository.saveAndFlush(Matching.start(requesterId, receiverId, 1, 1)).getId();
        em.createQuery("update Matching m set m.createdAt = :createdAt where m.id = :id")
                .setParameter("createdAt", Instant.parse(createdAt))
                .setParameter("id", id)
                .executeUpdate();
        em.clear();
        return id;
    }

    private void saveMessage(Long matchingId, String content, String createdAt) {
        Long id = chatMessageRepository.saveAndFlush(ChatMessage.user(matchingId, ME, content, null)).getId();
        em.createQuery("update ChatMessage c set c.createdAt = :createdAt where c.id = :id")
                .setParameter("createdAt", Instant.parse(createdAt))
                .setParameter("id", id)
                .executeUpdate();
        em.clear();
    }

    private void setStatus(Long matchingId, MatchingStatus status) {
        em.createQuery("update Matching m set m.status = :status where m.id = :id")
                .setParameter("status", status)
                .setParameter("id", matchingId)
                .executeUpdate();
        em.clear();
    }
}
