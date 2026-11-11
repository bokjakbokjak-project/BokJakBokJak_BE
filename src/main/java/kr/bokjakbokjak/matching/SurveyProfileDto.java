package kr.bokjakbokjak.matching;

import java.util.List;

/**
 * 사용자의 최신 설문 내용. 답변은 코드가 아니라 선택지 문구로 담는다.
 *
 * @param surveyRevision 설문 차수. 값이 바뀌었을 때만 요약을 다시 만들면 된다.
 * @param topCategories 선택한 카테고리 중 1~3순위 (순위 오름차순)
 * @param topics 주제 8개 (설문 순서)
 */
public record SurveyProfileDto(
        Long userId,
        int surveyRevision,
        List<CategoryRank> topCategories,
        List<TopicAnswer> topics) {

    public SurveyProfileDto {
        topCategories = List.copyOf(topCategories);
        topics = List.copyOf(topics);
    }

    public record CategoryRank(String code, String name, int rank) {
    }

    public record TopicAnswer(
            String topicCode,
            String categoryCode,
            String categoryName,
            String scenarioText,
            String habitQuestion,
            String habitAnswer,
            String toleranceQuestion,
            String toleranceAnswer) {
    }
}
