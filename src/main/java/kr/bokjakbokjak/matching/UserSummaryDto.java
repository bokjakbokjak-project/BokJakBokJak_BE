package kr.bokjakbokjak.matching;

import java.util.List;

public record UserSummaryDto(
        Long userId,
        String nickname,
        String department,
        String status,
        int surveyRevision,
        List<String> topCategories) {

    public UserSummaryDto {
        topCategories = List.copyOf(topCategories);
    }
}
