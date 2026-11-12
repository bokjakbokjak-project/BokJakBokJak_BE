package kr.bokjakbokjak.recommendation.dto;

import java.util.List;

public record RecommendationListResponse(List<RecommendationItem> recommendations, int totalCount) {
    public RecommendationListResponse {
        recommendations = List.copyOf(recommendations);
    }

    public static RecommendationListResponse of(List<RecommendationItem> recommendations) {
        return new RecommendationListResponse(recommendations, recommendations.size());
    }
}
