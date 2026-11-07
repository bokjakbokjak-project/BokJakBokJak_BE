package kr.bokjakbokjak.matching;

import java.util.List;

public record CandidateDto(
        Long userId,
        String nickname,
        String department,
        Integer entryYear,
        List<String> topCategories) {

    public CandidateDto {
        topCategories = List.copyOf(topCategories);
    }
}
