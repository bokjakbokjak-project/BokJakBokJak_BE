package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.recommendation.dto.RecommendationItem;
import kr.bokjakbokjak.recommendation.dto.RecommendationListResponse;
import kr.bokjakbokjak.recommendation.dto.RecommendationRefreshResponse;
import kr.bokjakbokjak.recommendation.dto.RecommendationRelation;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class RecommendationServiceTest {
    private static final long VIEWER = 7L;
    private static final long MINT = 12L;
    private static final long DAWN = 13L;
    private static final long CLOVER = 14L;
    private static final long SUNNY = 15L;
    private static final long MATCHED = 16L;
    private static final long PARTNER_OF_MATCHED = 17L;

    @MockitoBean MatchingEngine matchingEngine;

    @Autowired RecommendationService recommendationService;
    @Autowired FakeUserQueryService users;
    @Autowired InMemoryRecommendationSnapshotStore store;
    @Autowired MatchingRepository matchingRepository;
    @Autowired JdbcClient jdbc;

    private final Map<Long, String> scores = new HashMap<>();

    @DynamicPropertySource
    static void config(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret-base64", () -> Base64.getEncoder().encodeToString(new byte[32]));
    }

    @BeforeEach
    void setUp() {
        users.clear();
        store.clear();
        users.addUser(VIEWER, "책갈피", 1);
        for (long candidate : new long[] {MINT, DAWN, CLOVER, SUNNY, MATCHED}) {
            users.addUser(candidate, "후보" + candidate, 1);
            users.connect(VIEWER, candidate);
        }
        users.addUser(PARTNER_OF_MATCHED, "후보17", 1);
        scores.putAll(Map.of(MINT, "0.82", DAWN, "0.6", CLOVER, "0.9", SUNNY, "0.6", MATCHED, "0.99"));
        given(matchingEngine.calculate(anyLong(), anyLong())).willAnswer(invocation -> {
            Long target = invocation.getArgument(1);
            return new MatchResultDto(invocation.getArgument(0), target, new BigDecimal(scores.get(target)),
                    BigDecimal.ONE, BigDecimal.ONE, List.of());
        });
    }

    @AfterEach
    void cleanUp() {
        matchingRepository.deleteAll();
        users.clear();
        store.clear();
    }

    @Test
    void listsCandidatesByScoreExcludingMatchedUsers() {
        setStatus(start(MATCHED, PARTNER_OF_MATCHED), MatchingStatus.CONFIRMED);

        RecommendationListResponse response = recommendationService.getRecommendations(VIEWER);

        assertThat(response.totalCount()).isEqualTo(4);
        assertThat(response.recommendations())
                .extracting(RecommendationItem::userId, RecommendationItem::matchScore)
                .containsExactly(tuple(CLOVER, 90), tuple(MINT, 82), tuple(DAWN, 60), tuple(SUNNY, 60));
        assertThat(response.recommendations().getFirst()).satisfies(item -> {
            assertThat(item.nickname()).isEqualTo("후보14");
            assertThat(item.department()).isEqualTo("컴퓨터공학과");
            assertThat(item.age()).isNull();
            assertThat(item.topCategories()).containsExactly("CLEANING", "SLEEP_SCHEDULE", "SMOKING");
        });
    }

    @Test
    void relationShowsChattingAndEndedPartners() {
        start(MINT, VIEWER);
        setStatus(start(VIEWER, DAWN), MatchingStatus.ENDED);

        List<RecommendationItem> items = recommendationService.getRecommendations(VIEWER).recommendations();

        assertThat(items).extracting(RecommendationItem::userId, RecommendationItem::relation)
                .contains(tuple(MINT, RecommendationRelation.CHATTING),
                        tuple(DAWN, RecommendationRelation.ENDED),
                        tuple(CLOVER, RecommendationRelation.NONE));
    }

    @Test
    void refreshReportsWhetherRankingChanged() {
        recommendationService.getRecommendations(VIEWER);

        RecommendationRefreshResponse same = recommendationService.refresh(VIEWER);
        scores.put(SUNNY, "1.0");
        RecommendationRefreshResponse changed = recommendationService.refresh(VIEWER);

        assertThat(same.changed()).isFalse();
        assertThat(changed.changed()).isTrue();
        assertThat(changed.recommendations().getFirst().userId()).isEqualTo(SUNNY);
        assertThat(changed.totalCount()).isEqualTo(5);
    }

    @Test
    void noCandidatesReturnsEmptyList() {
        users.clear();
        users.addUser(VIEWER, "책갈피", 1);

        RecommendationListResponse response = recommendationService.getRecommendations(VIEWER);

        assertThat(response.recommendations()).isEmpty();
        assertThat(response.totalCount()).isZero();
    }

    @Test
    void viewerMustCompleteSurvey() {
        users.addUser(VIEWER, "책갈피", 0);

        ApiException exception = assertThrows(ApiException.class,
                () -> recommendationService.getRecommendations(VIEWER));

        assertThat(exception.code()).isEqualTo("ONBOARDING_REQUIRED");
        assertThat(exception.status()).isEqualTo(HttpStatus.CONFLICT);
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
