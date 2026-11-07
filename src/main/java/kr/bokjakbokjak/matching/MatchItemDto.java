package kr.bokjakbokjak.matching;

import java.math.BigDecimal;

public record MatchItemDto(
        String topicCode,
        String categoryCode,
        String scenarioText,
        BigDecimal score,
        MatchLabel label,
        BigDecimal viewerWeight,
        BigDecimal targetWeight,
        String viewerHabit,
        String viewerTolerance,
        String targetHabit,
        String targetTolerance) {
}
