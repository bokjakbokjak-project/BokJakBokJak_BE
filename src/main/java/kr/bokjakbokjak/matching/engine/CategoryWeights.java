package kr.bokjakbokjak.matching.engine;

/** 카테고리 가중치: 모든 카테고리 기본 10점 + 선택 순위 보너스. */
final class CategoryWeights {
    static final double BASE = 10;
    private static final double[] RANK_BONUS = {12, 6, 2, 1, 0.5};

    private CategoryWeights() {
    }

    /** rank가 null이면 선택하지 않은 카테고리. */
    static double of(Integer rank) {
        if (rank == null) {
            return BASE;
        }
        if (rank < 1 || rank > RANK_BONUS.length) {
            throw new IllegalArgumentException("지원하지 않는 순위입니다: " + rank);
        }
        return BASE + RANK_BONUS[rank - 1];
    }
}
