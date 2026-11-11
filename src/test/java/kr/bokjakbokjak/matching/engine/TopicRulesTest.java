package kr.bokjakbokjak.matching.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import kr.bokjakbokjak.matching.engine.TopicRules.Judgment;
import org.junit.jupiter.api.Test;

class TopicRulesTest {

    @Test
    void habitInsideToleranceIsFullScore() {
        assertThat(TopicRules.judge("FLOOR_CLEAN", "DAILY", "WEEKLY_MIN")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("FLOOR_CLEAN", "WEEKLY", "WEEKLY_MIN")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("BEDTIME", "FROM_23_TO_24", "BY_24")).isEqualTo(Judgment.FIT);
    }

    @Test
    void scoreDropsByQuarterPerStepOutsideTolerance() {
        assertThat(TopicRules.judge("FLOOR_CLEAN", "TWO_THREE_WEEKLY", "DAILY_MIN").score()).isEqualTo(0.75);
        assertThat(TopicRules.judge("FLOOR_CLEAN", "WEEKLY", "DAILY_MIN").score()).isEqualTo(0.5);
        assertThat(TopicRules.judge("FLOOR_CLEAN", "BIWEEKLY", "DAILY_MIN").score()).isEqualTo(0.25);
        assertThat(TopicRules.judge("FLOOR_CLEAN", "RARELY", "DAILY_MIN").score()).isEqualTo(0);
        assertThat(TopicRules.judge("BEDTIME", "AFTER_02", "BY_23").score()).isEqualTo(0);

        Judgment oneHourLate = TopicRules.judge("BEDTIME", "FROM_00_TO_01", "BY_24");
        assertThat(oneHourLate.score()).isEqualTo(0.75);
        assertThat(oneHourLate.outOfRange()).isTrue();
    }

    @Test
    void specialOptions() {
        // 해당 습관이 없으면 상대 허용범위와 무관하게 맞음
        assertThat(TopicRules.judge("ALARM", "NO_ALARM", "ONCE_ONLY")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("DISHES", "NOT_IN_ROOM", "IMMEDIATELY_ONLY")).isEqualTo(Judgment.FIT);
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
    void smokingUsesCombinationTable() {
        assertThat(TopicRules.judge("SMOKING", "NON_SMOKER", "NON_SMOKER_ONLY")).isEqualTo(Judgment.FIT);
        assertThat(TopicRules.judge("SMOKING", "QUIT_IN_DORM", "PREFER_NON_SMOKER")).isEqualTo(Judgment.CONDITIONAL);
        assertThat(TopicRules.judge("SMOKING", "QUIT_IN_DORM", "NON_SMOKER_ONLY")).isEqualTo(Judgment.MISMATCH);
        assertThat(TopicRules.judge("SMOKING", "OUTDOOR_SMOKER", "OUTSIDE_LOW_ODOR")).isEqualTo(Judgment.CONDITIONAL);
        assertThat(TopicRules.judge("SMOKING", "OUTDOOR_SMOKER", "PREFER_NON_SMOKER")).isEqualTo(Judgment.MISMATCH);
    }

    @Test
    void unknownOptionIsRejected() {
        assertThrows(IllegalStateException.class, () -> TopicRules.judge("ALARM", "SOMETIMES", "ONCE_ONLY"));
        assertThrows(IllegalStateException.class, () -> TopicRules.judge("NOISE", "A", "B"));
    }
}
