package kr.bokjakbokjak.matching;

/** Internal scoring contract for recommendations, AI analysis, and discussion topics. */
public interface MatchingEngine {
    MatchResultDto calculate(Long viewerId, Long targetId);
}
