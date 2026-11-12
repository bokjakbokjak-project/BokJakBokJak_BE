package kr.bokjakbokjak.recommendation.service;

import java.util.Map;
import kr.bokjakbokjak.matching.MatchItemDto;

final class DefaultMatchSentences {
    private static final Map<String, String> TOPIC_NAMES = Map.of(
            "SLEEP_HABITS", "잠버릇",
            "BEDTIME", "잠드는 시간",
            "ALARM", "아침 알람",
            "FLOOR_CLEAN", "바닥 청소",
            "FOOD_WASTE", "음식물 쓰레기 처리",
            "BELONGINGS", "개인 물건 정리",
            "DISHES", "식기 정리",
            "SMOKING", "흡연");

    private DefaultMatchSentences() {
    }

    static String of(MatchItemDto item) {
        String topic = TOPIC_NAMES.getOrDefault(item.topicCode(), "생활 습관");
        return switch (item.label()) {
            case GOOD -> "%s 기준이 비슷해요.".formatted(topic);
            case NEED_ADJUST -> "%s 기준이 서로 달라요. 대화에서 미리 정해 두면 좋아요.".formatted(topic);
            case NEED_CHECK -> "%s 질문에 '잘 모르겠다'는 답이 있어요. 대화에서 확인해 보세요.".formatted(topic);
        };
    }
}
