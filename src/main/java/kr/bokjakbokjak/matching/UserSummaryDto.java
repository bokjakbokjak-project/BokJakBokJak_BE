package kr.bokjakbokjak.matching;

import java.util.List;

/**
 * @param birthYear 출생년도. 나이는 한국식(올해 - 출생년도 + 1)으로 계산한다
 * @param status ACTIVE / WITHDRAWN / SUSPENDED
 * @param surveyRevision 설문을 낼 때마다 오르는 차수. 캐시가 최신인지 비교할 때 쓴다
 * @param surveyCompleted 설문 완료 여부. 설문을 마쳤는지는 차수 대신 이 값으로 판단한다
 * @param topCategories 선택한 카테고리 중 1~3순위 코드
 */
public record UserSummaryDto(
        Long userId,
        String nickname,
        String department,
        Integer birthYear,
        String status,
        int surveyRevision,
        boolean surveyCompleted,
        List<String> topCategories) {

    public UserSummaryDto {
        topCategories = List.copyOf(topCategories);
    }

    /** 설문 완료 여부를 차수로 정하는 생성자. 샘플 데이터와 탈퇴 사용자 표시용 */
    public UserSummaryDto(Long userId, String nickname, String department, Integer birthYear, String status,
            int surveyRevision, List<String> topCategories) {
        this(userId, nickname, department, birthYear, status, surveyRevision, surveyRevision > 0, topCategories);
    }
}
