package kr.bokjakbokjak.recommendation.service;

import java.time.Year;
import java.time.ZoneId;

final class KoreanAge {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private KoreanAge() {
    }

    static Integer of(Integer birthYear) {
        return of(birthYear, Year.now(SEOUL));
    }

    static Integer of(Integer birthYear, Year currentYear) {
        if (birthYear == null || birthYear > currentYear.getValue()) {
            return null;
        }
        return currentYear.getValue() - birthYear + 1;
    }
}
