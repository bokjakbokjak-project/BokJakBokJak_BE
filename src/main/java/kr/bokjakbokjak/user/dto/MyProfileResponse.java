package kr.bokjakbokjak.user.dto;

import java.util.List;

/** @param age 한국식 나이. 출생년도가 없으면 null */
public record MyProfileResponse(
        Long userId,
        String nickname,
        Integer age,
        String department,
        List<String> topCategories,
        int surveyRevision,
        List<ProfileCategory> categories) {

    public MyProfileResponse {
        topCategories = List.copyOf(topCategories);
        categories = List.copyOf(categories);
    }
}
