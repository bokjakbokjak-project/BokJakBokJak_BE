package kr.bokjakbokjak.room.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;

final class AgendaPrompt {
    static final String SYSTEM = """
            당신은 대학 기숙사 룸메이트 두 사람의 생활 규칙 조율을 돕는 사회자입니다.
            매칭 엔진이 판정한 주제별 결과를 받아, 두 사람이 채팅에서 이야기할 안건 2개를 만듭니다.

            규칙
            - 입력 순서대로 앞의 주제가 더 중요합니다. 앞의 두 주제로 안건을 만드세요.
            - title: 15자 이내의 짧은 상황 이름
            - content: 두 사람이 함께 정할 기준 하나만 묻는 질문 한 문장, 50자 이내, "~할까요?"로 끝나는 존댓말
            - content에 닉네임이나 각자의 습관을 쓰지 마세요. 두 사람에게 똑같이 묻는 질문이어야 합니다.
            - 괄호, 예시 답, 여러 개의 질문을 넣지 마세요.
            - 예/아니오로 답하는 질문 대신 시간, 횟수, 장소, 방법 같은 기준을 묻는 질문을 만드세요.
            - 기숙사 규칙상 실내 흡연과 음주는 금지입니다. 흡연 주제는 흡연 후 냄새, 옷과 소지품 관리처럼 함께 지낼 때의 배려 기준을 물으세요.
            - 점수나 판정을 다시 계산하지 마세요.
            - 코드블록 없이 아래 형식의 JSON 객체 하나만 출력하세요.
            {"agendas":[{"topic_code":"...","title":"...","content":"..."},{"topic_code":"...","title":"...","content":"..."}]}

            좋은 예
            {"topic_code":"BEDTIME","title":"평일 소등 시간","content":"평일에는 몇 시에 방 불을 끌까요?"}
            """;

    private static final Map<MatchLabel, String> LABEL_TEXT = Map.of(
            MatchLabel.GOOD, "잘 맞음",
            MatchLabel.NEED_ADJUST, "조율 필요",
            MatchLabel.NEED_CHECK, "확인 필요");

    private AgendaPrompt() {
    }

    static String userInput(List<MatchItemDto> topics) {
        String lines = IntStream.range(0, topics.size())
                .mapToObj(i -> {
                    MatchItemDto topic = topics.get(i);
                    return """
                            %d. topic_code: %s / 상황: %s / 판정: %s
                               - A 습관: %s / 허용: %s
                               - B 습관: %s / 허용: %s""".formatted(
                            i + 1, topic.topicCode(), topic.scenarioText(), LABEL_TEXT.get(topic.label()),
                            topic.viewerHabit(), topic.viewerTolerance(),
                            topic.targetHabit(), topic.targetTolerance());
                })
                .collect(Collectors.joining("\n"));
        return "두 사람: A, B\n" + lines;
    }
}
