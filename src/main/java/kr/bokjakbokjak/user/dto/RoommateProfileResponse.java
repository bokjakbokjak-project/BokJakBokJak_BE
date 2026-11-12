package kr.bokjakbokjak.user.dto;

import java.util.List;

/** 후보 본인의 답변만 담는다. 매칭율은 추천 리스트 값을 FE가 그대로 쓴다. */
public record RoommateProfileResponse(
        Long candidateId,
        String nickname,
        Integer age,
        String department,
        List<String> topCategories,
        int surveyRevision,
        List<ProfileCategory> categories) {

    public RoommateProfileResponse {
        topCategories = List.copyOf(topCategories);
        categories = List.copyOf(categories);
    }
}
