package kr.bokjakbokjak.recommendation.dto;

import java.time.Instant;
import java.util.List;

public record MatchAnalysisResponse(
        Target target,
        Integer matchScore,
        List<String> goodPoints,
        List<String> adjustPoints,
        List<String> checkPoints,
        Instant analyzedAt) {

    public MatchAnalysisResponse {
        goodPoints = List.copyOf(goodPoints);
        adjustPoints = List.copyOf(adjustPoints);
        checkPoints = List.copyOf(checkPoints);
    }

    public record Target(Long userId, String nickname, Integer age, String department, List<String> topCategories) {
        public Target {
            topCategories = List.copyOf(topCategories);
        }
    }
}
