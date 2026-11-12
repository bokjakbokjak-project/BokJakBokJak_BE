package kr.bokjakbokjak.room.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import kr.bokjakbokjak.matching.MatchItemDto;
import kr.bokjakbokjak.matching.MatchLabel;

final class AgendaTopicSelector {
    static final int AGENDA_COUNT = 2;

    private static final Map<MatchLabel, Integer> LABEL_PRIORITY = Map.of(
            MatchLabel.NEED_ADJUST, 0,
            MatchLabel.NEED_CHECK, 1,
            MatchLabel.GOOD, 2);

    private AgendaTopicSelector() {
    }

    static List<MatchItemDto> select(List<MatchItemDto> items) {
        return items.stream()
                .sorted(Comparator.comparing((MatchItemDto item) -> LABEL_PRIORITY.get(item.label()))
                        .thenComparing(item -> item.viewerWeight().add(item.targetWeight()), Comparator.reverseOrder()))
                .limit(AGENDA_COUNT)
                .toList();
    }
}
