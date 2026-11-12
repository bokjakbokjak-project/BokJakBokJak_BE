package kr.bokjakbokjak.room.service;

import java.util.Map;

final class DefaultAgendas {
    private static final GeneratedAgenda GENERAL = new GeneratedAgenda(null, "함께 지낼 규칙",
            "함께 지내면서 가장 먼저 정하고 싶은 규칙은 무엇일까요?");

    private static final Map<String, GeneratedAgenda> BY_TOPIC = Map.of(
            "SLEEP_HABITS", new GeneratedAgenda("SLEEP_HABITS", "잠버릇 배려",
                    "잠버릇 때문에 불편할 때는 어떻게 알려 줄까요?"),
            "BEDTIME", new GeneratedAgenda("BEDTIME", "평일 소등 시간",
                    "평일에는 몇 시에 방 불을 끌까요?"),
            "ALARM", new GeneratedAgenda("ALARM", "아침 알람",
                    "아침 알람은 몇 번까지 울리게 할까요?"),
            "FLOOR_CLEAN", new GeneratedAgenda("FLOOR_CLEAN", "바닥 청소 주기",
                    "방 바닥 청소는 얼마나 자주 할까요?"),
            "FOOD_WASTE", new GeneratedAgenda("FOOD_WASTE", "음식물 쓰레기",
                    "음식물 쓰레기는 언제까지 버릴까요?"),
            "BELONGINGS", new GeneratedAgenda("BELONGINGS", "개인 물건 정리",
                    "꺼내 둔 개인 물건은 언제까지 정리할까요?"),
            "DISHES", new GeneratedAgenda("DISHES", "식기 정리",
                    "사용한 컵이나 그릇은 언제까지 정리할까요?"),
            "SMOKING", new GeneratedAgenda("SMOKING", "흡연 후 배려",
                    "흡연 후 방에 들어올 때 어떤 점을 지킬까요?"));

    private DefaultAgendas() {
    }

    static GeneratedAgenda of(String topicCode) {
        return BY_TOPIC.getOrDefault(topicCode, GENERAL);
    }
}
