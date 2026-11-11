package kr.bokjakbokjak.matching.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;
import kr.bokjakbokjak.matching.MatchResultDto;

/**
 * 두 사용자의 적합도 계산 (DB와 무관한 순수 계산).
 *
 * viewer→target = Σ(viewer의 주제 가중치 × target 습관이 viewer 허용범위에 맞는 정도) ÷ Σ(viewer의 주제 가중치)
 * 최종 점수 = 두 방향의 조화평균 (둘 다 0이면 0)
 * 주제 가중치 = 카테고리 가중치 ÷ 카테고리에 속한 주제 수
 */
final class MatchScoreCalculator {
    private static final int SCORE_SCALE = 4;
    private static final int WEIGHT_SCALE = 2;

    record Topic(
            String topicCode,
            String categoryCode,
            String scenarioText,
            String habitQuestionCode,
            String toleranceQuestionCode) {
    }

    record Profile(Long userId, Map<String, Integer> rankByCategory, Map<String, String> optionByQuestion) {
        String option(String questionCode) {
            String option = optionByQuestion.get(questionCode);
            if (option == null) {
                throw new IllegalStateException("답변이 없는 문항입니다: user=%d, %s".formatted(userId, questionCode));
            }
            return option;
        }
    }

    private MatchScoreCalculator() {
    }

    /**
     * @param topics 표시 순서대로 정렬된 주제 목록
     * @param labels 문항 코드 → (선택지 코드 → 문구)
     */
    static MatchResultDto calculate(
            List<Topic> topics, Map<String, Map<String, String>> labels, Profile viewer, Profile target) {
        Map<String, Long> topicsPerCategory = topics.stream()
                .collect(Collectors.groupingBy(Topic::categoryCode, Collectors.counting()));

        double viewerScore = 0;
        double viewerTotal = 0;
        double targetScore = 0;
        double targetTotal = 0;
        List<MatchItemDto> items = new ArrayList<>();

        for (Topic topic : topics) {
            long topicCount = topicsPerCategory.get(topic.categoryCode());
            double viewerWeight = CategoryWeights.of(viewer.rankByCategory().get(topic.categoryCode())) / topicCount;
            double targetWeight = CategoryWeights.of(target.rankByCategory().get(topic.categoryCode())) / topicCount;

            String viewerHabit = viewer.option(topic.habitQuestionCode());
            String viewerTolerance = viewer.option(topic.toleranceQuestionCode());
            String targetHabit = target.option(topic.habitQuestionCode());
            String targetTolerance = target.option(topic.toleranceQuestionCode());

            // viewer 입장: 상대(target)의 습관이 내 허용범위에 맞는가
            TopicRules.Judgment forViewer = TopicRules.judge(topic.topicCode(), targetHabit, viewerTolerance);
            // target 입장: 나(viewer)의 습관이 상대 허용범위에 맞는가
            TopicRules.Judgment forTarget = TopicRules.judge(topic.topicCode(), viewerHabit, targetTolerance);

            viewerScore += viewerWeight * forViewer.score();
            viewerTotal += viewerWeight;
            targetScore += targetWeight * forTarget.score();
            targetTotal += targetWeight;

            items.add(new MatchItemDto(
                    topic.topicCode(),
                    topic.categoryCode(),
                    topic.scenarioText(),
                    score((forViewer.score() + forTarget.score()) / 2),
                    label(forViewer, forTarget),
                    weight(viewerWeight),
                    weight(targetWeight),
                    labelOf(labels, topic.habitQuestionCode(), viewerHabit),
                    labelOf(labels, topic.toleranceQuestionCode(), viewerTolerance),
                    labelOf(labels, topic.habitQuestionCode(), targetHabit),
                    labelOf(labels, topic.toleranceQuestionCode(), targetTolerance)));
        }

        double viewerToTarget = viewerScore / viewerTotal;
        double targetToViewer = targetScore / targetTotal;
        return new MatchResultDto(
                viewer.userId(),
                target.userId(),
                score(harmonicMean(viewerToTarget, targetToViewer)),
                score(viewerToTarget),
                score(targetToViewer),
                items);
    }

    static double harmonicMean(double first, double second) {
        if (first + second == 0) {
            return 0;
        }
        return 2 * first * second / (first + second);
    }

    private static MatchLabel label(TopicRules.Judgment forViewer, TopicRules.Judgment forTarget) {
        if (forViewer.outOfRange() || forTarget.outOfRange()) {
            return MatchLabel.NEED_ADJUST;
        }
        if (forViewer.needsCheck() || forTarget.needsCheck()) {
            return MatchLabel.NEED_CHECK;
        }
        return MatchLabel.GOOD;
    }

    private static String labelOf(Map<String, Map<String, String>> labels, String questionCode, String optionCode) {
        return labels.getOrDefault(questionCode, Map.of()).getOrDefault(optionCode, optionCode);
    }

    private static BigDecimal score(double value) {
        return BigDecimal.valueOf(value).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal weight(double value) {
        return BigDecimal.valueOf(value).setScale(WEIGHT_SCALE, RoundingMode.HALF_UP);
    }
}
