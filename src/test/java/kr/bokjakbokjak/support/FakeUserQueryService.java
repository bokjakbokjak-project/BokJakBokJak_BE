package kr.bokjakbokjak.support;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kr.bokjakbokjak.matching.CandidateDto;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** 테스트 프로필에서는 실구현(UserQueryServiceImpl)보다 이 가짜 구현을 우선 주입한다. */
@Primary
@Component
@Profile("test")
public class FakeUserQueryService implements UserQueryService {
    public static final String ACTIVE = "ACTIVE";
    public static final String WITHDRAWN = "WITHDRAWN";

    private final Map<Long, UserSummaryDto> users = new ConcurrentHashMap<>();
    private final Map<Long, Set<Long>> candidates = new ConcurrentHashMap<>();

    public void addUser(long userId, String nickname, int surveyRevision) {
        users.put(userId, new UserSummaryDto(userId, nickname, "컴퓨터공학과", 2005, ACTIVE, surveyRevision,
                List.of("CLEANING", "SLEEP_SCHEDULE", "SMOKING")));
    }

    public void withdraw(long userId) {
        UserSummaryDto user = users.get(userId);
        users.put(userId, new UserSummaryDto(userId, user.nickname(), user.department(), user.birthYear(), WITHDRAWN,
                user.surveyRevision(), user.topCategories()));
    }

    public void connect(long userId, long otherUserId) {
        candidates.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(otherUserId);
        candidates.computeIfAbsent(otherUserId, ignored -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    public void clear() {
        users.clear();
        candidates.clear();
    }

    @Override
    public List<CandidateDto> findCandidates(Long viewerId) {
        return candidates.getOrDefault(viewerId, Set.of()).stream()
                .sorted()
                .map(users::get)
                .filter(Objects::nonNull)
                .filter(user -> ACTIVE.equals(user.status()) && user.surveyRevision() > 0)
                .map(user -> new CandidateDto(user.userId(), user.nickname(), user.department(), 2024,
                        user.birthYear(), user.topCategories()))
                .toList();
    }

    @Override
    public List<UserSummaryDto> getUserSummaries(List<Long> userIds) {
        return userIds.stream()
                .distinct()
                .map(users::get)
                .filter(Objects::nonNull)
                .toList();
    }
}
