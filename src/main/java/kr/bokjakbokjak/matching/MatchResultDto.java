package kr.bokjakbokjak.matching;

import java.math.BigDecimal;
import java.util.List;

/**
 * Scores are ratios from 0 to 1.
 * viewerToTarget uses the viewer's weights and evaluates the target against the viewer's tolerance.
 * targetToViewer uses the target's weights and evaluates the viewer against the target's tolerance.
 */
public record MatchResultDto(
        Long viewerId,
        Long targetId,
        BigDecimal mutualScore,
        BigDecimal viewerToTarget,
        BigDecimal targetToViewer,
        List<MatchItemDto> items) {

    public MatchResultDto {
        items = List.copyOf(items);
    }
}
