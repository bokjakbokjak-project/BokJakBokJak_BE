package kr.bokjakbokjak.matching;

import java.util.List;

/**
 * @param birthYear 출생년도. 나이는 한국식(올해 - 출생년도 + 1)으로 계산한다
 * @param status ACTIVE / WITHDRAWN / SUSPENDED
 * @param topCategories 선택한 카테고리 중 1~3순위 코드
 */
public record UserSummaryDto(
        Long userId,
        String nickname,
        String department,
        Integer birthYear,
        String status,
        int surveyRevision,
        List<String> topCategories) {

    public UserSummaryDto {
        topCategories = List.copyOf(topCategories);
    }
}
