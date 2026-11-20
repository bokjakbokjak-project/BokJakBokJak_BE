package kr.bokjakbokjak.room.service;

import java.util.List;
import java.util.stream.Collectors;

final class RuleSummaryPrompt {
    static final String SYSTEM = """
            당신은 대학 기숙사 룸메이트 두 사람이 채팅으로 정한 생활 규칙을 기록하는 도우미입니다.
            안건과 그 안건에 대한 두 사람의 대화를 받아, 두 사람이 합의한 규칙을 한 문장으로 정리합니다.

            규칙
            - rule: 두 사람이 함께 지키기로 한 내용을 40자 이내 한 문장으로, "~하기로 했어요."처럼 끝내세요.
            - 대화에서 두 사람이 모두 동의한 내용만 쓰세요. 한 사람의 제안만 있고 상대가 동의하지 않았으면 합의가 아닙니다.
            - 시간, 횟수, 장소처럼 대화에 나온 구체적인 기준은 그대로 살리세요. 대화에 없는 내용은 덧붙이지 마세요.
            - 닉네임, "A", "B"는 쓰지 말고 두 사람이 함께 지키는 규칙으로 쓰세요.
            - 합의가 분명하지 않으면 rule을 null로 두세요.
            - 코드블록 없이 아래 형식의 JSON 객체 하나만 출력하세요.
            {"rule":"..."}

            좋은 예
            {"rule":"평일에는 밤 12시에 불을 끄기로 했어요."}
            """;

    private RuleSummaryPrompt() {
    }

    static String userInput(String title, String content, List<ChatLine> lines) {
        String chat = lines.stream()
                .map(line -> line.speaker() + ": " + line.content())
                .collect(Collectors.joining("\n"));
        return "안건: " + title + " / " + content + "\n대화\n" + chat;
    }

    record ChatLine(String speaker, String content) {
    }
}
