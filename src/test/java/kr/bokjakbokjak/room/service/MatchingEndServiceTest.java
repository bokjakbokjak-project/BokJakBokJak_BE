package kr.bokjakbokjak.room.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Base64;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.room.dto.MatchingEndResponse;
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
class MatchingEndServiceTest {
    private static final long BOOKMARK = 7L;
    private static final long MINT = 12L;
    private static final long OUTSIDER = 99L;

    @Autowired MatchingEndService matchingEndService;
    @Autowired FakeUserQueryService users;
    @Autowired MatchingRepository matchingRepository;
    @Autowired ChatMessageRepository chatMessageRepository;
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
        matchingId = matchingRepository.save(Matching.start(BOOKMARK, MINT, 1, 1)).getId();
    }

    @AfterEach
    void cleanUp() {
        chatMessageRepository.deleteAll();
        matchingRepository.deleteAll();
        users.clear();
    }

    @Test
    void endsChattingWithSystemMessage() {
        MatchingEndResponse response = matchingEndService.end(MINT, matchingId);

        assertThat(response.matchingId()).isEqualTo(matchingId);
        assertThat(response.status()).isEqualTo(MatchingStatus.ENDED);
        assertThat(response.endReason()).isEqualTo(EndReason.USER_ENDED);
        assertThat(response.endedAt()).isNotNull();
        assertThat(matchingRepository.findById(matchingId).orElseThrow().getEndedBy()).isEqualTo(MINT);
        assertThat(chatMessageRepository.findAll()).singleElement().satisfies(message -> {
            assertThat(message.getType()).isEqualTo(MessageType.SYSTEM);
            assertThat(message.getMatchingId()).isEqualTo(matchingId);
            assertThat(message.getContent()).isEqualTo("민트초코님이 매칭을 중지했어요.");
        });
    }

    @Test
    void onlyChattingMatchingCanEnd() {
        matchingEndService.end(BOOKMARK, matchingId);
        assertNotChatting();

        setStatus(MatchingStatus.CONFIRMED);
        assertNotChatting();
        assertThat(chatMessageRepository.count()).isEqualTo(1);
    }

    @Test
    void outsiderAndMissingRoomAreRejected() {
        ApiException forbidden = assertThrows(ApiException.class, () -> matchingEndService.end(OUTSIDER, matchingId));
        ApiException notFound = assertThrows(ApiException.class,
                () -> matchingEndService.end(BOOKMARK, Long.MAX_VALUE));

        assertThat(forbidden.code()).isEqualTo("MATCHING_FORBIDDEN");
        assertThat(notFound.code()).isEqualTo("MATCHING_NOT_FOUND");
    }

    private void assertNotChatting() {
        ApiException exception = assertThrows(ApiException.class, () -> matchingEndService.end(BOOKMARK, matchingId));
        assertThat(exception.code()).isEqualTo("INVALID_MATCHING_STATUS");
        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getMessage()).isEqualTo("조율 중인 매칭만 중지할 수 있습니다.");
    }

    private void setStatus(MatchingStatus status) {
        jdbc.sql("update matching set status = ? where matching_id = ?")
                .params(status.name(), matchingId)
                .update();
    }
}
