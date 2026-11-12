package kr.bokjakbokjak.recommendation.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import kr.bokjakbokjak.matching.MatchingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class RecommendationScorer {
    private static final Logger log = LoggerFactory.getLogger(RecommendationScorer.class);
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    private final MatchingEngine matchingEngine;

    RecommendationScorer(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    Integer score(Long viewerId, Long candidateId) {
        try {
            return toPercent(matchingEngine.calculate(viewerId, candidateId).mutualScore());
        } catch (IllegalArgumentException | IllegalStateException exception) {
            log.warn("Match score unavailable: viewer={}, candidate={}", viewerId, candidateId);
            return null;
        }
    }

    static Integer toPercent(BigDecimal ratio) {
        return ratio == null ? null : ratio.multiply(PERCENT).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}
