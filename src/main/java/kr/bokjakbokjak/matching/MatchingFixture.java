package kr.bokjakbokjak.matching;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Fixed sample data for development against the A/B contract. */
@Component
@Profile("matching-fixture")
final class MatchingFixture implements UserQueryService, MatchingEngine {
    private static final long FIRST_ID = 7L;
    private static final long SECOND_ID = 12L;
    private static final long WITHDRAWN_ID = 99L;

    private static final Map<Long, UserSummaryDto> USERS = Map.of(
            FIRST_ID, new UserSummaryDto(
                    FIRST_ID, "책갈피", "산업공학과", 2004, "ACTIVE", 1,
                    List.of("CLEANING", "SLEEP_HABITS", "SMOKING")),
            SECOND_ID, new UserSummaryDto(
                    SECOND_ID, "민트초코", "컴퓨터공학과", 2003, "ACTIVE", 3,
                    List.of("SMOKING", "CLEANING", "PERSONAL_HYGIENE")),
            WITHDRAWN_ID, new UserSummaryDto(
                    WITHDRAWN_ID, "이전 사용자", "기계공학과", 2002, "WITHDRAWN", 2, List.of()));

    private static final Map<Long, CandidateDto> CANDIDATES = Map.of(
            FIRST_ID, new CandidateDto(FIRST_ID, "책갈피", "산업공학과", 2024, 2004,
                    USERS.get(FIRST_ID).topCategories()),
            SECOND_ID, new CandidateDto(SECOND_ID, "민트초코", "컴퓨터공학과", 2023, 2003,
                    USERS.get(SECOND_ID).topCategories()));

    private static final BigDecimal FIRST_PERSPECTIVE_SCORE = new BigDecimal("0.85");
    private static final BigDecimal SECOND_PERSPECTIVE_SCORE = new BigDecimal("0.79");
    private static final BigDecimal MUTUAL_SCORE = new BigDecimal("0.82");
    private static final BigDecimal FIRST_WEIGHT = new BigDecimal("1.8");
    private static final BigDecimal SECOND_WEIGHT = new BigDecimal("1.0");

    @Override
    public List<CandidateDto> findCandidates(Long viewerId) {
        if (Objects.equals(viewerId, FIRST_ID)) {
            return List.of(CANDIDATES.get(SECOND_ID));
        }
        if (Objects.equals(viewerId, SECOND_ID)) {
            return List.of(CANDIDATES.get(FIRST_ID));
        }
        return List.of();
    }

    @Override
    public List<UserSummaryDto> getUserSummaries(List<Long> userIds) {
        return userIds.stream()
                .distinct()
                .map(USERS::get)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public MatchResultDto calculate(Long viewerId, Long targetId) {
        boolean firstIsViewer = Objects.equals(viewerId, FIRST_ID) && Objects.equals(targetId, SECOND_ID);
        boolean secondIsViewer = Objects.equals(viewerId, SECOND_ID) && Objects.equals(targetId, FIRST_ID);
        if (!firstIsViewer && !secondIsViewer) {
            throw new IllegalArgumentException("The matching fixture supports only the sample pair 7 and 12");
        }

        String firstHabit = "다음 날 정리하기도 한다";
        String firstTolerance = "그날 안에만 정리하면 괜찮다";
        String secondHabit = "이틀 이상 두는 경우도 있다";
        String secondTolerance = "위생 문제가 없다면 더 두어도 괜찮다";

        var item = new MatchItemDto(
                "DISHES", "PERSONAL_HYGIENE", "방 안 책상 위에 사용한 컵이나 그릇 같은 식기가 놓여 있어요.",
                new BigDecimal("0.5"), MatchLabel.NEED_ADJUST,
                firstIsViewer ? FIRST_WEIGHT : SECOND_WEIGHT,
                firstIsViewer ? SECOND_WEIGHT : FIRST_WEIGHT,
                firstIsViewer ? firstHabit : secondHabit,
                firstIsViewer ? firstTolerance : secondTolerance,
                firstIsViewer ? secondHabit : firstHabit,
                firstIsViewer ? secondTolerance : firstTolerance);

        return new MatchResultDto(
                viewerId, targetId, MUTUAL_SCORE,
                firstIsViewer ? FIRST_PERSPECTIVE_SCORE : SECOND_PERSPECTIVE_SCORE,
                firstIsViewer ? SECOND_PERSPECTIVE_SCORE : FIRST_PERSPECTIVE_SCORE,
                List.of(item));
    }
}
