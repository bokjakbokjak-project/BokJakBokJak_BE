package kr.bokjakbokjak.recommendation.service;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kr.bokjakbokjak.matching.SurveyProfileDto;

final class LifestyleSummaryPrompt {
    static final String SYSTEM = """
            당신은 대학 기숙사 룸메이트 매칭 서비스에서 한 사람의 생활습관 설문을 짧게 소개하는 도우미입니다. 이 요약은 본인과 다른 사용자가 함께 봅니다.

            규칙
            - summary: 2문장, 각 문장 45자 이내, 모든 문장을 "~해요"로 끝내세요. "~습니다" 같은 다른 말투와 "저는" 같은 1인칭, 주어와 닉네임은 쓰지 마세요.
            - 본인의 습관 답변만 쓰고, 허용 답변(룸메이트에게 바라는 점)은 쓰지 마세요.
            - 중요 카테고리(1순위부터) 관련 습관을 먼저 쓰세요.
            - 같은 내용을 반복하거나 풀어 쓰지 말고, 한 문장에는 습관을 두 개까지만 담으세요.
            - 답변에 없는 내용이나 분위기, 감정을 덧붙이지 마세요.
            - "잘 모르겠다", "일정하지 않다" 답변은 summary와 keywords에 쓰지 마세요.
            - 게으르다, 예민하다처럼 사람을 평가하는 표현과 좋다/나쁘다는 판단은 쓰지 마세요.
            - keywords: 3~5개, 각 10자 이내, 괄호 없이 띄어쓰기를 지킨 짧은 명사형으로 쓰세요. 1~3순위 카테고리마다 하나씩 먼저 고르고, 남으면 다른 영역에서 고르세요. 같은 영역에서 두 개를 고르지 마세요.
            - 코드블록 없이 아래 형식의 JSON 객체 하나만 출력하세요.
            {"summary":"...","keywords":["..."]}

            좋은 예
            {"summary":"밤 11시에서 12시 사이에 잠들고 알람은 한두 번이면 일어나는 편이에요. 사용한 식기와 음식물은 그날 안에 정리해요.","keywords":["12시 전 취침","알람 1~2번","그날 정리","비흡연"]}
            """;

    private LifestyleSummaryPrompt() {
    }

    static String userInput(SurveyProfileDto profile) {
        String categories = profile.topCategories().stream()
                .map(category -> category.rank() + "순위 " + category.name())
                .collect(Collectors.joining(", "));
        String topics = IntStream.range(0, profile.topics().size())
                .mapToObj(i -> {
                    SurveyProfileDto.TopicAnswer topic = profile.topics().get(i);
                    return "%d. %s / %s → 습관: %s / 허용: %s".formatted(
                            i + 1, topic.categoryName(), topic.habitQuestion(),
                            topic.habitAnswer(), topic.toleranceAnswer());
                })
                .collect(Collectors.joining("\n"));
        return "중요 카테고리: " + categories + "\n" + topics;
    }
}
