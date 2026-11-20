package kr.bokjakbokjak.matching.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.engine.MatchScoreCalculator.Profile;
import kr.bokjakbokjak.matching.engine.MatchScoreCalculator.Topic;
import org.junit.jupiter.api.Test;

class MatchScoreCalculatorTest {
    private static final List<Topic> TOPICS = List.of(
            topic("SLEEP_HABITS", "SLEEP_HABITS"),
            topic("BEDTIME", "SLEEP_SCHEDULE"),
            topic("ALARM", "SLEEP_SCHEDULE"),
            topic("FLOOR_CLEAN", "CLEANING"),
            topic("FOOD_WASTE", "CLEANING"),
            topic("BELONGINGS", "PERSONAL_HYGIENE"),
            topic("SMOKING", "SMOKING"),
            topic("TEMPERATURE", "TEMPERATURE"));

    // 청소 1순위(22점), 기상 알람 2순위(16점), 개인 청결 3순위(12점), 나머지 10점 → 총 80점
    private static final Profile A = new Profile(1L,
            Map.of("CLEANING", 1, "SLEEP_SCHEDULE", 2, "PERSONAL_HYGIENE", 3),
            Map.ofEntries(
                    Map.entry("SLEEP_HABITS_H", "RARE"), Map.entry("SLEEP_HABITS_T", "OCCASIONAL_OK"),
                    Map.entry("BEDTIME_H", "FROM_22_TO_24"), Map.entry("BEDTIME_T", "BY_01"),
                    Map.entry("ALARM_H", "ONCE"), Map.entry("ALARM_T", "UP_TO_2"),
                    Map.entry("FLOOR_CLEAN_H", "WEEKLY"), Map.entry("FLOOR_CLEAN_T", "WEEKLY_MIN"),
                    Map.entry("FOOD_WASTE_H", "SAME_DAY"), Map.entry("FOOD_WASTE_T", "SAME_DAY_OK"),
                    Map.entry("BELONGINGS_H", "IMMEDIATELY"), Map.entry("BELONGINGS_T", "SAME_DAY_OK"),
                    Map.entry("SMOKING_H", "NON_SMOKER"), Map.entry("SMOKING_T", "ANY_OK"),
                    Map.entry("TEMPERATURE_H", "LONG"), Map.entry("TEMPERATURE_T", "LONG_OK")));

    // A와 같은 습관이지만 취침만 늦고(A 허용범위보다 1시간), 허용범위는 넓다
    private static final Profile B = new Profile(2L,
            Map.of("SMOKING", 1, "SLEEP_HABITS", 2, "CLEANING", 3),
            Map.ofEntries(
                    Map.entry("SLEEP_HABITS_H", "RARE"), Map.entry("SLEEP_HABITS_T", "FREQUENT_OK"),
                    Map.entry("BEDTIME_H", "FROM_00_TO_02"), Map.entry("BEDTIME_T", "BY_02"),
                    Map.entry("ALARM_H", "ONCE"), Map.entry("ALARM_T", "MANY_OK"),
                    Map.entry("FLOOR_CLEAN_H", "WEEKLY"), Map.entry("FLOOR_CLEAN_T", "WHEN_NEEDED"),
                    Map.entry("FOOD_WASTE_H", "SAME_DAY"), Map.entry("FOOD_WASTE_T", "NO_ISSUE_OK"),
                    Map.entry("BELONGINGS_H", "IMMEDIATELY"), Map.entry("BELONGINGS_T", "FEW_DAYS_OK"),
                    Map.entry("SMOKING_H", "NON_SMOKER"), Map.entry("SMOKING_T", "ANY_OK"),
                    Map.entry("TEMPERATURE_H", "LONG"), Map.entry("TEMPERATURE_T", "ALWAYS_ON_OK")));

    @Test
    void matchesHandCalculation() {
        MatchResultDto result = MatchScoreCalculator.calculate(TOPICS, Map.of(), A, B);

        // A→B: 취침(8점)만 1시간 늦어 1 − 1/3 → (72 + 8 × 2/3) / 80 = 0.966667
        assertThat(result.viewerToTarget()).isEqualByComparingTo("0.9667");
        // B→A: 전부 허용범위 안
        assertThat(result.targetToViewer()).isEqualByComparingTo("1.0000");
        // 조화평균: 2 × 0.966667 × 1 / 1.966667
        assertThat(result.mutualScore()).isEqualByComparingTo("0.9831");
    }

    @Test
    void itemsCarryLabelsAndWeights() {
        MatchResultDto result = MatchScoreCalculator.calculate(TOPICS, Map.of(), A, B);
        Map<String, MatchItemDto> items = result.items().stream()
                .collect(java.util.stream.Collectors.toMap(MatchItemDto::topicCode, item -> item));

        assertThat(result.items()).extracting(MatchItemDto::topicCode).containsExactly(
                "SLEEP_HABITS", "BEDTIME", "ALARM", "FLOOR_CLEAN", "FOOD_WASTE", "BELONGINGS",
                "SMOKING", "TEMPERATURE");
        assertThat(items.get("SLEEP_HABITS").label()).isEqualTo(MatchLabel.GOOD);
        assertThat(items.get("BEDTIME").label()).isEqualTo(MatchLabel.NEED_ADJUST);
        assertThat(items.get("BEDTIME").score()).isEqualByComparingTo("0.8333");
        assertThat(items.get("FOOD_WASTE").label()).isEqualTo(MatchLabel.NEED_CHECK);

        // 기상 알람 2순위 16점을 주제 2개가 나눠 가짐
        assertThat(items.get("BEDTIME").viewerWeight()).isEqualByComparingTo("8");
        assertThat(items.get("FLOOR_CLEAN").viewerWeight()).isEqualByComparingTo("11");
        // 식기가 빠져 개인 청결 3순위 12점은 개인 물건 하나가 가짐
        assertThat(items.get("BELONGINGS").viewerWeight()).isEqualByComparingTo("12");
        assertThat(items.get("SMOKING").targetWeight()).isEqualByComparingTo("22");
        assertThat(items.get("BEDTIME").targetHabit()).isEqualTo("FROM_00_TO_02");
    }

    @Test
    void swappingUsersSwapsDirections() {
        MatchResultDto forward = MatchScoreCalculator.calculate(TOPICS, Map.of(), A, B);
        MatchResultDto reversed = MatchScoreCalculator.calculate(TOPICS, Map.of(), B, A);

        assertThat(reversed.viewerToTarget()).isEqualByComparingTo(forward.targetToViewer());
        assertThat(reversed.targetToViewer()).isEqualByComparingTo(forward.viewerToTarget());
        assertThat(reversed.mutualScore()).isEqualByComparingTo(forward.mutualScore());
    }

    @Test
    void smokingConflictLowersFinalScore() {
        Profile nonSmokerOnly = with(A, "SMOKING_T", "NON_SMOKER_ONLY");
        Profile outdoorSmoker = with(A, "SMOKING_H", "OUTDOOR_SMOKER");

        MatchResultDto result = MatchScoreCalculator.calculate(TOPICS, Map.of(), nonSmokerOnly, outdoorSmoker);

        // 흡연(10점)만 0점 → 70 / 80, 반대 방향은 1 → 조화평균 0.933333 × 0.6
        assertThat(result.viewerToTarget()).isEqualByComparingTo("0.8750");
        assertThat(result.targetToViewer()).isEqualByComparingTo("1.0000");
        assertThat(result.mutualScore()).isEqualByComparingTo("0.5600");
    }

    @Test
    void missingAnswersCountAsUnknown() {
        Map<String, String> answers = new HashMap<>(A.optionByQuestion());
        answers.remove("TEMPERATURE_H");
        answers.remove("TEMPERATURE_T");
        Profile beforeResurvey = new Profile(3L, A.rankByCategory(), answers);

        MatchResultDto result = MatchScoreCalculator.calculate(
                TOPICS, Map.of("TEMPERATURE_H", Map.of("LONG", "덥거나 추울 때 일정시간 이상 켜둔다")), A, beforeResurvey);

        // 온도(10점)만 0.5점 → 75 / 80, 양쪽 같음
        assertThat(result.viewerToTarget()).isEqualByComparingTo("0.9375");
        assertThat(result.mutualScore()).isEqualByComparingTo("0.9375");
        MatchItemDto temperature = result.items().get(result.items().size() - 1);
        assertThat(temperature.label()).isEqualTo(MatchLabel.NEED_CHECK);
        assertThat(temperature.viewerHabit()).isEqualTo("덥거나 추울 때 일정시간 이상 켜둔다");
        assertThat(temperature.targetHabit()).isEqualTo(MatchScoreCalculator.NOT_ANSWERED);
    }

    @Test
    void harmonicMeanIsZeroWhenEitherSideIsZero() {
        assertThat(MatchScoreCalculator.harmonicMean(0, 0)).isZero();
        assertThat(MatchScoreCalculator.harmonicMean(1, 0)).isZero();
        assertThat(BigDecimal.valueOf(MatchScoreCalculator.harmonicMean(1, 0.4)))
                .isEqualByComparingTo(BigDecimal.valueOf(0.8 / 1.4));
    }

    private static Profile with(Profile profile, String questionCode, String optionCode) {
        Map<String, String> answers = new HashMap<>(profile.optionByQuestion());
        answers.put(questionCode, optionCode);
        return new Profile(profile.userId() + 10, profile.rankByCategory(), answers);
    }

    private static Topic topic(String topicCode, String categoryCode) {
        return new Topic(topicCode, categoryCode, "", topicCode + "_H", topicCode + "_T");
    }
}
