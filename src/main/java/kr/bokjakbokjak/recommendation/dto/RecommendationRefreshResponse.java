package kr.bokjakbokjak.recommendation.dto;

import java.util.List;

public record RecommendationRefreshResponse(boolean changed, List<RecommendationItem> recommendations,
        int totalCount) {
    public RecommendationRefreshResponse {
        recommendations = List.copyOf(recommendations);
    }

    public static RecommendationRefreshResponse of(boolean changed, List<RecommendationItem> recommendations) {
        return new RecommendationRefreshResponse(changed, recommendations, recommendations.size());
    }
}
