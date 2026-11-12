package kr.bokjakbokjak.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Year;
import org.junit.jupiter.api.Test;

class KoreanAgeTest {
    private static final Year YEAR_2026 = Year.of(2026);

    @Test
    void countsBirthYearAsOne() {
        assertThat(KoreanAge.of(2006, YEAR_2026)).isEqualTo(21);
        assertThat(KoreanAge.of(2026, YEAR_2026)).isEqualTo(1);
    }

    @Test
    void unknownOrFutureBirthYearHasNoAge() {
        assertThat(KoreanAge.of(null, YEAR_2026)).isNull();
        assertThat(KoreanAge.of(2027, YEAR_2026)).isNull();
    }
}
