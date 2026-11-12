package kr.bokjakbokjak.recommendation.dto;

import java.util.List;

public record RecommendationItem(
        Long userId,
        String nickname,
        String department,
        Integer age,
        List<String> topCategories,
        Integer matchScore,
        RecommendationRelation relation) {

    public RecommendationItem {
        topCategories = List.copyOf(topCategories);
    }
}
