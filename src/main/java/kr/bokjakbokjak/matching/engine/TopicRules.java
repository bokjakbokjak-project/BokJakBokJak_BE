package kr.bokjakbokjak.matching.engine;

import java.util.Map;
import java.util.Set;

/**
 * 주제별 판정 규칙.
 * 한 사람의 실제 습관(HABIT 답변)이 다른 사람의 허용범위(TOLERANCE 답변) 안에 드는지 점수로 매긴다.
 * 허용범위를 벗어나면 한 칸마다 0.25점씩 깎는다 (1 → 0.75 → 0.5 → 0.25 → 0).
 */
final class TopicRules {
    static final double STEP_PENALTY = 0.25;

    /** score: 0~1, outOfRange: 허용범위를 벗어남, needsCheck: 조건부·모름이라 대화로 확인이 필요함 */
    record Judgment(double score, boolean outOfRange, boolean needsCheck) {
        static final Judgment FIT = new Judgment(1, false, false);
        static final Judgment CONDITIONAL = new Judgment(1, false, true);
        static final Judgment UNKNOWN = new Judgment(0.5, false, true);
        static final Judgment MISMATCH = new Judgment(0, true, false);

        static Judgment outside(int steps) {
            return new Judgment(Math.max(0, 1 - STEP_PENALTY * steps), true, false);
        }
    }

    /**
     * habitLevels: 값이 클수록 상대에게 부담이 큰 습관
     * toleranceLimits: 이 값까지는 괜찮다는 허용 한도
     * alwaysFit: 상대 허용범위와 무관하게 맞는 습관 (예: 방에서 음식을 먹지 않음)
     * unknown: 본인도 모르는 습관 (0.5점)
     * conditional: 조건만 지키면 상관없다는 허용범위 (1점, 확인 필요)
     */
    private record Scale(
            Map<String, Integer> habitLevels,
            Map<String, Integer> toleranceLimits,
            Set<String> alwaysFit,
            Set<String> unknown,
            Set<String> conditional) {
    }

    private static final Scale CLEAN_UP_TIMING = new Scale(
            Map.of("IMMEDIATELY", 0, "SAME_DAY", 1, "NEXT_DAY", 2, "TWO_DAYS_PLUS", 3),
            Map.of("IMMEDIATELY_ONLY", 0, "SAME_DAY_OK", 1, "NEXT_DAY_OK", 2),
            Set.of("NOT_IN_ROOM"),
            Set.of(),
            Set.of("NO_ISSUE_OK"));

    private static final Map<String, Scale> SCALES = Map.of(
            "SLEEP_HABITS", new Scale(
                    Map.of("NONE", 0, "RARE", 1, "OCCASIONAL", 2, "FREQUENT", 3),
                    Map.of("NONE_ONLY", 0, "RARE_OK", 1, "OCCASIONAL_OK", 2, "FREQUENT_OK", 3),
                    Set.of(), Set.of("UNKNOWN"), Set.of()),
            // 잠드는 시간대의 늦은 쪽 끝 시각 기준, 1시간 = 1칸 (22시 0, 24시 2, 2시 4)
            "BEDTIME", new Scale(
                    Map.of("BEFORE_22", 0, "FROM_22_TO_24", 2, "FROM_00_TO_02", 4, "AFTER_02", 5),
                    Map.of("BY_23", 1, "BY_24", 2, "BY_01", 3, "BY_02", 4),
                    Set.of(), Set.of("IRREGULAR"), Set.of("QUIET_ANY")),
            "ALARM", new Scale(
                    Map.of("ONCE", 1, "AT_MOST_2", 2, "THREE_TO_FOUR", 3, "FIVE_OR_MORE", 4),
                    Map.of("ONCE_ONLY", 1, "UP_TO_2", 2, "UP_TO_4", 3, "MANY_OK", 4),
                    Set.of("NO_ALARM"), Set.of(), Set.of()),
            // 청소를 드물게 할수록 값이 크다
            "FLOOR_CLEAN", new Scale(
                    Map.of("DAILY", 0, "TWO_THREE_WEEKLY", 1, "WEEKLY", 2, "BIWEEKLY", 3, "RARELY", 4),
                    Map.of("DAILY_MIN", 0, "TWO_THREE_WEEKLY_MIN", 1, "WEEKLY_MIN", 2,
                            "BIWEEKLY_MIN", 3, "WHEN_NEEDED", 4),
                    Set.of(), Set.of(), Set.of()),
            "FOOD_WASTE", CLEAN_UP_TIMING,
            "BELONGINGS", new Scale(
                    Map.of("IMMEDIATELY", 0, "SAME_DAY", 1, "FEW_DAYS", 2, "WHEN_NEEDED", 3),
                    Map.of("IMMEDIATELY_ONLY", 0, "SAME_DAY_OK", 1, "FEW_DAYS_OK", 2),
                    Set.of(), Set.of(), Set.of("NO_SPACE_INVASION")),
            "DISHES", CLEAN_UP_TIMING);

    /** 흡연은 단계보다 조합별 의미가 커서 표로 판정한다. */
    private static final Map<String, Map<String, Judgment>> SMOKING = Map.of(
            "NON_SMOKER", Map.of(
                    "ANY_OK", Judgment.FIT,
                    "OUTSIDE_LOW_ODOR", Judgment.FIT,
                    "PREFER_NON_SMOKER", Judgment.FIT,
                    "NON_SMOKER_ONLY", Judgment.FIT),
            "QUIT_IN_DORM", Map.of(
                    "ANY_OK", Judgment.FIT,
                    "OUTSIDE_LOW_ODOR", Judgment.FIT,
                    "PREFER_NON_SMOKER", Judgment.CONDITIONAL,
                    "NON_SMOKER_ONLY", Judgment.MISMATCH),
            "OUTDOOR_SMOKER", Map.of(
                    "ANY_OK", Judgment.FIT,
                    "OUTSIDE_LOW_ODOR", Judgment.CONDITIONAL,
                    "PREFER_NON_SMOKER", Judgment.MISMATCH,
                    "NON_SMOKER_ONLY", Judgment.MISMATCH));

    private TopicRules() {
    }

    static Judgment judge(String topicCode, String habitOption, String toleranceOption) {
        if ("SMOKING".equals(topicCode)) {
            Judgment judgment = SMOKING.getOrDefault(habitOption, Map.of()).get(toleranceOption);
            if (judgment == null) {
                throw unknownOption(topicCode, habitOption, toleranceOption);
            }
            return judgment;
        }

        Scale scale = SCALES.get(topicCode);
        if (scale == null) {
            throw new IllegalStateException("판정 규칙이 없는 주제입니다: " + topicCode);
        }
        if (scale.alwaysFit().contains(habitOption)) {
            return Judgment.FIT;
        }
        if (scale.conditional().contains(toleranceOption)) {
            return Judgment.CONDITIONAL;
        }
        if (scale.unknown().contains(habitOption)) {
            return Judgment.UNKNOWN;
        }

        Integer habit = scale.habitLevels().get(habitOption);
        Integer limit = scale.toleranceLimits().get(toleranceOption);
        if (habit == null || limit == null) {
            throw unknownOption(topicCode, habitOption, toleranceOption);
        }
        int steps = habit - limit;
        return steps <= 0 ? Judgment.FIT : Judgment.outside(steps);
    }

    private static IllegalStateException unknownOption(String topicCode, String habit, String tolerance) {
        return new IllegalStateException(
                "판정할 수 없는 선택지입니다: %s (%s, %s)".formatted(topicCode, habit, tolerance));
    }
}
