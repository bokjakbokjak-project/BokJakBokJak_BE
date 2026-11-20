package kr.bokjakbokjak.matching.engine;

import java.util.Map;
import java.util.Set;

/**
 * 주제별 판정 규칙.
 * 한 사람의 실제 습관(HABIT 답변)이 다른 사람의 허용범위(TOLERANCE 답변) 안에 드는지 점수로 매긴다.
 * 선택지를 실제 값(시각, 횟수, 시간, 일수)으로 바꿔서 허용범위를 얼마나 벗어났는지로 점수를 정한다.
 * 실제 단위가 없는 주제(잠버릇, 온도)는 선택지 칸 수로 계산한다.
 */
final class TopicRules {
    /** 허용 한도가 없음 (예: 여러 번 울려도 괜찮다) */
    private static final double NO_LIMIT = Double.POSITIVE_INFINITY;

    /** score: 0~1, outOfRange: 허용범위를 벗어남, needsCheck: 조건부·모름·미응답이라 대화로 확인이 필요함 */
    record Judgment(double score, boolean outOfRange, boolean needsCheck) {
        static final Judgment FIT = new Judgment(1, false, false);
        static final Judgment CONDITIONAL = new Judgment(1, false, true);
        static final Judgment UNKNOWN = new Judgment(0.5, false, true);
        static final Judgment MISMATCH = new Judgment(0, true, false);

        static Judgment outside(double score) {
            return new Judgment(Math.max(0, Math.min(1, score)), true, false);
        }
    }

    /** 습관 값이 허용 한도를 넘었을 때의 점수 */
    private enum Fit {
        /** 허용 한도 ÷ 실제 값. 알람 횟수, 치우기까지 걸리는 시간, 청소 간격처럼 몇 배 차이인지가 중요한 경우 */
        RATIO,
        /** 1 − 넘은 양 ÷ span. 취침 시각, 칸 단위 주제처럼 차이가 일정하게 쌓이는 경우 */
        LINEAR
    }

    /**
     * habits: 실제 값. 클수록 상대에게 부담이 크다
     * limits: 이 값까지는 괜찮다는 허용 한도
     * span: LINEAR에서 0점이 되는 초과량
     * alwaysFit: 상대 허용범위와 무관하게 맞는 습관 (예: 방에서 음식을 먹지 않음)
     * unknown: 본인도 모르는 습관 (0.5점)
     * conditional: 조건만 지키면 상관없다는 허용범위 (1점, 확인 필요)
     */
    private record Scale(
            Map<String, Double> habits,
            Map<String, Double> limits,
            Fit fit,
            double span,
            Set<String> alwaysFit,
            Set<String> unknown,
            Set<String> conditional) {

        double score(double habit, double limit) {
            return fit == Fit.RATIO ? limit / habit : 1 - (habit - limit) / span;
        }
    }

    private static final Map<String, Scale> SCALES = Map.of(
            // 칸 단위, 한 칸에 0.5점
            "SLEEP_HABITS", new Scale(
                    Map.of("NONE", 0.0, "RARE", 1.0, "OCCASIONAL", 2.0, "FREQUENT", 3.0),
                    Map.of("NONE_ONLY", 0.0, "RARE_OK", 1.0, "OCCASIONAL_OK", 2.0, "FREQUENT_OK", 3.0),
                    Fit.LINEAR, 2, Set.of(), Set.of("UNKNOWN"), Set.of()),
            // 잠드는 시간대의 늦은 쪽 끝 시각 (24시 = 24, 새벽 2시 = 26). 3시간 늦으면 0점
            "BEDTIME", new Scale(
                    Map.of("BEFORE_22", 22.0, "FROM_22_TO_24", 24.0, "FROM_00_TO_02", 26.0, "AFTER_02", 27.0),
                    Map.of("BY_23", 23.0, "BY_24", 24.0, "BY_01", 25.0, "BY_02", 26.0),
                    Fit.LINEAR, 3, Set.of(), Set.of("IRREGULAR"), Set.of("QUIET_ANY")),
            // 알람이 울리는 횟수 (3~4번 = 4, 5번 이상 = 6)
            "ALARM", new Scale(
                    Map.of("ONCE", 1.0, "AT_MOST_2", 2.0, "THREE_TO_FOUR", 4.0, "FIVE_OR_MORE", 6.0),
                    Map.of("ONCE_ONLY", 1.0, "UP_TO_2", 2.0, "UP_TO_4", 4.0, "MANY_OK", NO_LIMIT),
                    Fit.RATIO, 0, Set.of("NO_ALARM"), Set.of(), Set.of()),
            // 청소 간격(일). 주 2~3회 = 2.8일, 더 드물게 = 28일
            "FLOOR_CLEAN", new Scale(
                    Map.of("DAILY", 1.0, "TWO_THREE_WEEKLY", 2.8, "WEEKLY", 7.0, "BIWEEKLY", 14.0, "RARELY", 28.0),
                    Map.of("DAILY_MIN", 1.0, "TWO_THREE_WEEKLY_MIN", 2.8, "WEEKLY_MIN", 7.0,
                            "BIWEEKLY_MIN", 14.0, "WHEN_NEEDED", NO_LIMIT),
                    Fit.RATIO, 0, Set.of(), Set.of(), Set.of()),
            // 치우기까지 걸리는 시간(시간). 그날 = 12, 다음 날 = 36, 이틀 이상 = 72
            "FOOD_WASTE", new Scale(
                    Map.of("IMMEDIATELY", 0.0, "SAME_DAY", 12.0, "NEXT_DAY", 36.0, "TWO_DAYS_PLUS", 72.0),
                    Map.of("IMMEDIATELY_ONLY", 1.0, "SAME_DAY_OK", 12.0, "NEXT_DAY_OK", 36.0),
                    Fit.RATIO, 0, Set.of("NOT_IN_ROOM"), Set.of(), Set.of("NO_ISSUE_OK")),
            // 정리하기까지 걸리는 시간(시간). 며칠 = 72, 필요할 때 = 120
            "BELONGINGS", new Scale(
                    Map.of("IMMEDIATELY", 0.0, "SAME_DAY", 12.0, "FEW_DAYS", 72.0, "WHEN_NEEDED", 120.0),
                    Map.of("IMMEDIATELY_ONLY", 1.0, "SAME_DAY_OK", 12.0, "FEW_DAYS_OK", 96.0),
                    Fit.RATIO, 0, Set.of(), Set.of(), Set.of("NO_SPACE_INVASION")),
            // 냉난방을 켜두는 정도. 칸 단위, 한 칸에 1/3점
            "TEMPERATURE", new Scale(
                    Map.of("RARELY", 0.0, "SHORT", 1.0, "LONG", 2.0, "ALWAYS_ON", 3.0),
                    Map.of("RARELY_ONLY", 0.0, "SHORT_ONLY", 1.0, "LONG_OK", 2.0, "ALWAYS_ON_OK", 3.0),
                    Fit.LINEAR, 3, Set.of(), Set.of(), Set.of()));

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

    /** 흡연자를 받을 수 없는 사람과 흡연자가 만난 경우. 다른 주제가 잘 맞아도 최종 점수를 낮춘다 */
    static boolean isSmokingConflict(String topicCode, Judgment judgment) {
        return "SMOKING".equals(topicCode) && judgment == Judgment.MISMATCH;
    }

    /** 답이 없는 쪽이 있으면(설문 개편 후 다시 설문하기 전) 모름과 같이 0.5점, 확인 필요로 본다. */
    static Judgment judge(String topicCode, String habitOption, String toleranceOption) {
        if (habitOption == null || toleranceOption == null) {
            return Judgment.UNKNOWN;
        }
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

        Double habit = scale.habits().get(habitOption);
        Double limit = scale.limits().get(toleranceOption);
        if (habit == null || limit == null) {
            throw unknownOption(topicCode, habitOption, toleranceOption);
        }
        return habit <= limit ? Judgment.FIT : Judgment.outside(scale.score(habit, limit));
    }

    private static IllegalStateException unknownOption(String topicCode, String habit, String tolerance) {
        return new IllegalStateException(
                "판정할 수 없는 선택지입니다: %s (%s, %s)".formatted(topicCode, habit, tolerance));
    }
}
