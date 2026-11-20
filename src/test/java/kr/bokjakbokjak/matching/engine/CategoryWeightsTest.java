package kr.bokjakbokjak.matching.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CategoryWeightsTest {

    @Test
    void rankBonusGoesUpToSixthRank() {
        assertThat(CategoryWeights.of(1)).isEqualTo(22);
        assertThat(CategoryWeights.of(5)).isEqualTo(10.5);
        assertThat(CategoryWeights.of(6)).isEqualTo(10.25);
        assertThat(CategoryWeights.of(null)).isEqualTo(10);
    }

    @Test
    void rejectsUnsupportedRank() {
        assertThrows(IllegalArgumentException.class, () -> CategoryWeights.of(0));
        assertThrows(IllegalArgumentException.class, () -> CategoryWeights.of(7));
    }
}
