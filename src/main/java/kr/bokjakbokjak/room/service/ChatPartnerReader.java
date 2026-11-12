package kr.bokjakbokjak.room.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import org.springframework.stereotype.Component;

@Component
class ChatPartnerReader {
    private static final String UNKNOWN_NICKNAME = "알 수 없는 사용자";

    private final UserQueryService userQueryService;

    ChatPartnerReader(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    Map<Long, UserSummaryDto> readAll(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserSummaryDto> found = userQueryService.getUserSummaries(List.copyOf(userIds)).stream()
                .collect(Collectors.toMap(UserSummaryDto::userId, Function.identity(), (first, second) -> first));
        return userIds.stream().distinct()
                .collect(Collectors.toMap(Function.identity(), id -> found.getOrDefault(id, unknown(id))));
    }

    UserSummaryDto read(Long userId) {
        return readAll(List.of(userId)).get(userId);
    }

    private static UserSummaryDto unknown(Long userId) {
        return new UserSummaryDto(userId, UNKNOWN_NICKNAME, null, null, 0, List.of());
    }
}
