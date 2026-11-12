package kr.bokjakbokjak.recommendation.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;

final class MatchAnalysisPrompt {
    static final String SYSTEM = """
            당신은 룸메이트 매칭 결과를 설명하는 도우미입니다. viewer가 target의 추천 결과를 볼 때 보여줄 문장을 주제별로 씁니다.

            규칙
            - 입력의 주제마다 정확히 한 개의 항목을 쓰세요. 입력에 없는 주제를 만들지 마세요.
            - 입력의 "나"는 viewer, "상대"는 target입니다. 두 사람을 함께 말할 때는 "두 분", target은 "상대", viewer는 "회원님"이라고 쓰고 닉네임은 쓰지 마세요.
            - 판정에 따라 아래 형태로 쓰세요.
              - 잘 맞음: "두 분 모두 ~해서 ~이 비슷해요." 한 문장
              - 조율 필요: "~이 달라요. ~를 미리 정해 두면 좋아요." 두 문장
              - 확인 필요: "잘 모르겠다"고 답한 쪽이 나이면 "회원님이 ~를 '잘 모르겠다'고 답하셨어요. 대화에서 확인해 보세요.", 상대이면 "상대가 ~를 '잘 모르겠다'고 답했어요. 대화에서 확인해 보세요." 두 사람 모두라면 "두 분 모두 ~를 '잘 모르겠다'고 답했어요. 대화에서 함께 정해 보세요."
            - 시간, 횟수 같은 구체적인 습관 값은 쓰지 마세요. 모든 문장은 "~해요" 체이고 마침표로 끝내세요.
            - 코드블록 없이 아래 형식의 JSON 객체 하나만 출력하세요.
            {"items":[{"topic_code":"...","sentence":"..."}]}

            좋은 예
            {"items":[{"topic_code":"FLOOR_CLEAN","sentence":"두 분 모두 바닥 청소를 자주 하는 편이라 청결 기준이 비슷해요."},{"topic_code":"DISHES","sentence":"사용한 식기를 치우는 시점이 달라요. 언제까지 둘지 미리 정해 두면 좋아요."},{"topic_code":"SLEEP_HABITS","sentence":"상대가 잠버릇 질문에 '잘 모르겠다'고 답했어요. 대화에서 확인해 보세요."}]}
            """;

    private static final Map<MatchLabel, String> LABEL_TEXT = Map.of(
            MatchLabel.GOOD, "잘 맞음",
            MatchLabel.NEED_ADJUST, "조율 필요",
            MatchLabel.NEED_CHECK, "확인 필요");

    private MatchAnalysisPrompt() {
    }

    static String userInput(List<MatchItemDto> items) {
        return IntStream.range(0, items.size())
                .mapToObj(i -> {
                    MatchItemDto item = items.get(i);
                    return """
                            %d. topic_code: %s / 상황: %s / 판정: %s
                            - 나 습관: %s / 허용: %s
                            - 상대 습관: %s / 허용: %s""".formatted(
                            i + 1, item.topicCode(), item.scenarioText(), LABEL_TEXT.get(item.label()),
                            item.viewerHabit(), item.viewerTolerance(),
                            item.targetHabit(), item.targetTolerance());
                })
                .collect(Collectors.joining("\n"));
    }
}
