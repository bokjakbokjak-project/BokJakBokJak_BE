package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

class RecommendationCalculatorTest {
    private static final long VIEWER = 7L;

    private final Map<Long, String> scores = new HashMap<>();
    private final List<Long> calculated = new ArrayList<>();
    private InMemoryRecommendationSnapshotStore store;
    private RecommendationCalculator calculator;

    @BeforeEach
    void setUp() {
        MatchingEngine engine = (viewerId, targetId) -> {
            calculated.add(targetId);
            String score = scores.get(targetId);
            if (score == null) {
                throw new IllegalArgumentException("설문을 완료하지 않은 사용자입니다: " + targetId);
            }
            return new MatchResultDto(viewerId, targetId, new BigDecimal(score), BigDecimal.ONE, BigDecimal.ONE,
                    List.of());
        };
        store = new InMemoryRecommendationSnapshotStore();
        calculator = new RecommendationCalculator(new RecommendationScorer(engine), store);
    }

    @Test
    void ranksByScoreThenUserIdWithUnknownScoresLast() {
        scores.put(12L, "0.8237");
        scores.put(13L, "0.5");
        scores.put(14L, "0.8249");

        RecommendationCalculator.Result result = calculator.calculate(VIEWER, 1,
                revisions(15L, 1, 14L, 1, 13L, 1, 12L, 1), false);

        assertThat(result.ranking()).containsExactly(
                new RankedCandidate(12L, 82),
                new RankedCandidate(14L, 82),
                new RankedCandidate(13L, 50),
                new RankedCandidate(15L, null));
        assertThat(result.changed()).isTrue();
    }

    @Test
    void reusesScoresUntilSurveyChanges() {
        scores.put(12L, "0.8");
        scores.put(13L, "0.6");
        calculator.calculate(VIEWER, 1, revisions(12L, 1, 13L, 1), false);
        calculated.clear();

        calculator.calculate(VIEWER, 1, revisions(12L, 1, 13L, 2, 14L, 1), false);
        assertThat(calculated).containsExactlyInAnyOrder(13L, 14L);

        calculated.clear();
        calculator.calculate(VIEWER, 2, revisions(12L, 1, 13L, 2, 14L, 1), false);
        assertThat(calculated).containsExactlyInAnyOrder(12L, 13L, 14L);
    }

    @Test
    void refreshRecalculatesAllAndReportsChanges() {
        scores.put(12L, "0.8");
        scores.put(13L, "0.6");
        calculator.calculate(VIEWER, 1, revisions(12L, 1, 13L, 1), false);
        calculated.clear();

        RecommendationCalculator.Result same = calculator.calculate(VIEWER, 1, revisions(12L, 1, 13L, 1), true);
        assertThat(calculated).containsExactlyInAnyOrder(12L, 13L);
        assertThat(same.changed()).isFalse();

        scores.put(13L, "0.9");
        RecommendationCalculator.Result reordered = calculator.calculate(VIEWER, 1, revisions(12L, 1, 13L, 1), true);
        assertThat(reordered.changed()).isTrue();
        assertThat(reordered.ranking()).extracting(RankedCandidate::userId).containsExactly(13L, 12L);
    }

    @Test
    void snapshotSurvivesJsonRoundTrip() {
        scores.put(12L, "0.8");
        calculator.calculate(VIEWER, 3, revisions(12L, 2, 13L, 1), false);
        RecommendationSnapshot snapshot = store.find(VIEWER).orElseThrow();
        var mapper = JsonMapper.builder().propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE).build();

        String json = mapper.writeValueAsString(snapshot);

        assertThat(mapper.readValue(json, RecommendationSnapshot.class)).isEqualTo(snapshot);
    }

    private static Map<Long, Integer> revisions(Object... pairs) {
        Map<Long, Integer> revisions = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            revisions.put((Long) pairs[i], (Integer) pairs[i + 1]);
        }
        return revisions;
    }
}
