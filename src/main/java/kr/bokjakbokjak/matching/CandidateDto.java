package kr.bokjakbokjak.matching;

import java.util.List;

/**
 * @param entryYear 학번 앞 4자리. 학번 형식이 맞지 않으면 null
 * @param birthYear 출생년도. 나이는 한국식(올해 - 출생년도 + 1)으로 계산한다
 * @param topCategories 선택한 카테고리 중 1~3순위 코드
 */
public record CandidateDto(
        Long userId,
        String nickname,
        String department,
        Integer entryYear,
        Integer birthYear,
        List<String> topCategories) {

    public CandidateDto {
        topCategories = List.copyOf(topCategories);
    }
}
