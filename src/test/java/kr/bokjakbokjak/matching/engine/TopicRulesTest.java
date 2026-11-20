package kr.bokjakbokjak.matching.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;

import kr.bokjakbokjak.matching.engine.TopicRules.Judgment;
import org.junit.jupiter.api.Test;

class TopicRulesTest {

    @Test
    void habitInsideToleranceIsFullScore() {
        assertThat(TopicRules.judge("FLOOR_CLEAN", "DAILY", "WEEKLY_MIN")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("FLOOR_CLEAN", "WEEKLY", "WEEKLY_MIN")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("FLOOR_CLEAN", "RARELY", "WHEN_NEEDED")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("BEDTIME", "FROM_22_TO_24", "BY_24")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("BEDTIME", "FROM_00_TO_02", "BY_02")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("ALARM", "FIVE_OR_MORE", "MANY_OK")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("FOOD_WASTE", "IMMEDIATELY", "IMMEDIATELY_ONLY")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("TEMPERATURE", "RARELY", "RARELY_ONLY")).isEqualTo(Judgment.FIT);
    }

    @Test
    void bedtimeLosesOneThirdPerHourLate() {
        // 새벽 12시~2시는 끝 시각인 새벽 2시 기준이라 새벽 1시 마감보다 1시간, 12시 마감보다 2시간 늦다
        Judgment oneHourLate = TopicRules.judge("BEDTIME", "FROM_00_TO_02", "BY_01");
        assertThat(oneHourLate.score()).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(oneHourLate.outOfRange()).isTrue();
        assertThat(TopicRules.judge("BEDTIME", "FROM_00_TO_02", "BY_24").score()).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(TopicRules.judge("BEDTIME", "FROM_22_TO_24", "BY_23").score()).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(TopicRules.judge("BEDTIME", "AFTER_02", "BY_23").score()).isZero();
    }

    @Test
    void countsAndDelaysUseRatioOfLimitToHabit() {
        // 알람: 2번까지 괜찮은데 3~4번(4) 울리면 2/4
        assertThat(TopicRules.judge("ALARM", "THREE_TO_FOUR", "UP_TO_2").score()).isEqualTo(0.5);
        assertThat(TopicRules.judge("ALARM", "FIVE_OR_MORE", "UP_TO_2").score())
                .isCloseTo(1.0 / 3, within(1e-9));
        // 청소 간격: 주 2~3회(2.8일)를 원하는데 주 1회(7일)
        assertThat(TopicRules.judge("FLOOR_CLEAN", "WEEKLY", "TWO_THREE_WEEKLY_MIN").score())
                .isCloseTo(0.4, within(1e-9));
        assertThat(TopicRules.judge("FLOOR_CLEAN", "RARELY", "WEEKLY_MIN").score()).isEqualTo(0.25);
        // 음식물: 그날(12시간)까지 괜찮은데 다음 날(36시간)
        assertThat(TopicRules.judge("FOOD_WASTE", "NEXT_DAY", "SAME_DAY_OK").score())
                .isCloseTo(1.0 / 3, within(1e-9));
        assertThat(TopicRules.judge("FOOD_WASTE", "SAME_DAY", "IMMEDIATELY_ONLY").score())
                .isCloseTo(1.0 / 12, within(1e-9));
        // 개인 물건: 며칠(96시간)까지 괜찮은데 필요할 때(120시간)
        assertThat(TopicRules.judge("BELONGINGS", "WHEN_NEEDED", "FEW_DAYS_OK").score()).isCloseTo(0.8, within(1e-9));
    }

    @Test
    void stepTopicsUseFixedPenaltyPerStep() {
        // 잠버릇은 한 칸에 0.5점
        assertThat(TopicRules.judge("SLEEP_HABITS", "OCCASIONAL", "RARE_OK").score()).isEqualTo(0.5);
        assertThat(TopicRules.judge("SLEEP_HABITS", "FREQUENT", "NONE_ONLY").score()).isZero();
        // 냉난방은 한 칸에 1/3점
        assertThat(TopicRules.judge("TEMPERATURE", "ALWAYS_ON", "LONG_OK").score()).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(TopicRules.judge("TEMPERATURE", "ALWAYS_ON", "SHORT_ONLY").score()).isCloseTo(1.0 / 3, within(1e-9));
        assertThat(TopicRules.judge("TEMPERATURE", "ALWAYS_ON", "RARELY_ONLY").score()).isZero();
        assertThat(TopicRules.judge("TEMPERATURE", "SHORT", "RARELY_ONLY").outOfRange()).isTrue();
    }

    @Test
    void specialOptions() {
        // 해당 습관이 없으면 상대 허용범위와 무관하게 맞음
        assertThat(TopicRules.judge("ALARM", "NO_ALARM", "ONCE_ONLY")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("FOOD_WASTE", "NOT_IN_ROOM", "IMMEDIATELY_ONLY")).isEqualTo(Judgment.FIT);
        // 조건부 허용은 맞음으로 보되 확인 필요
        assertThat(TopicRules.judge("FOOD_WASTE", "TWO_DAYS_PLUS", "NO_ISSUE_OK")).isEqualTo(Judgment.CONDITIONAL);
        assertThat(TopicRules.judge("BEDTIME", "IRREGULAR", "QUIET_ANY")).isEqualTo(Judgment.CONDITIONAL);
        // 본인도 모르는 습관은 0.5점, 확인 필요
        Judgment unknown = TopicRules.judge("SLEEP_HABITS", "UNKNOWN", "NONE_ONLY");
        assertThat(unknown.score()).isEqualTo(0.5);
        assertThat(unknown.needsCheck()).isTrue();
        assertThat(unknown.outOfRange()).isFalse();
    }

    @Test
    void missingAnswerIsTreatedAsUnknown() {
        assertThat(TopicRules.judge("TEMPERATURE", null, "LONG_OK")).isEqualTo(Judgment.UNKNOWN);
        assertThat(TopicRules.judge("TEMPERATURE", "LONG", null)).isEqualTo(Judgment.UNKNOWN);
        assertThat(TopicRules.judge("SMOKING", null, "ANY_OK")).isEqualTo(Judgment.UNKNOWN);
    }

    @Test
    void smokingUsesCombinationTable() {
        assertThat(TopicRules.judge("SMOKING", "NON_SMOKER", "NON_SMOKER_ONLY")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("SMOKING", "QUIT_IN_DORM", "PREFER_NON_SMOKER")).isEqualTo(Judgment.CONDITIONAL);
        assertThat(TopicRules.judge("SMOKING", "QUIT_IN_DORM", "NON_SMOKER_ONLY")).isEqualTo(Judgment.MISMATCH);
        assertThat(TopicRules.judge("SMOKING", "OUTDOOR_SMOKER", "OUTSIDE_LOW_ODOR")).isEqualTo(Judgment.CONDITIONAL);
        assertThat(TopicRules.judge("SMOKING", "OUTDOOR_SMOKER", "PREFER_NON_SMOKER")).isEqualTo(Judgment.MISMATCH);

        assertThat(TopicRules.isSmokingConflict("SMOKING", Judgment.MISMATCH)).isTrue();
        assertThat(TopicRules.isSmokingConflict("SMOKING", Judgment.CONDITIONAL)).isFalse();
        Judgment zeroTemperature = TopicRules.judge("TEMPERATURE", "ALWAYS_ON", "RARELY_ONLY");
        assertThat(TopicRules.isSmokingConflict("TEMPERATURE", zeroTemperature)).isFalse();
    }

    @Test
    void unknownOptionIsRejected() {
        assertThrows(IllegalStateException.class, () -> TopicRules.judge("ALARM", "SOMETIMES", "ONCE_ONLY"));
        assertThrows(IllegalStateException.class, () -> TopicRules.judge("DISHES", "SAME_DAY", "SAME_DAY_OK"));
    }
}
