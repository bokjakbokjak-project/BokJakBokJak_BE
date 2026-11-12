package kr.bokjakbokjak.recommendation.service;

import java.time.Year;
import java.time.ZoneId;

/** 한국식 나이(올해 - 출생년도 + 1). 추천 카드와 프로필이 같은 기준을 쓴다. */
public final class KoreanAge {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private KoreanAge() {
    }

    public static Integer of(Integer birthYear) {
        return of(birthYear, Year.now(SEOUL));
    }

    static Integer of(Integer birthYear, Year currentYear) {
        if (birthYear == null || birthYear > currentYear.getValue()) {
            return null;
        }
        return currentYear.getValue() - birthYear + 1;
    }
}
