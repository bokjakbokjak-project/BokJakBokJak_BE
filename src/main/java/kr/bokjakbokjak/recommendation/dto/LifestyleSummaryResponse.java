package kr.bokjakbokjak.recommendation.dto;

import java.time.Instant;
import java.util.List;

public record LifestyleSummaryResponse(Long userId, String summary, List<String> keywords, Instant generatedAt) {
    public LifestyleSummaryResponse {
        keywords = List.copyOf(keywords);
    }
}
