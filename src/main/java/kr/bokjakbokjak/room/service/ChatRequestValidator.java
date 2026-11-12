package kr.bokjakbokjak.room.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ChatRequestValidator {
    private static final String WITHDRAWN = "WITHDRAWN";

    private final UserQueryService userQueryService;
    private final MatchingRepository matchingRepository;

    ChatRequestValidator(UserQueryService userQueryService, MatchingRepository matchingRepository) {
        this.userQueryService = userQueryService;
        this.matchingRepository = matchingRepository;
    }

    @Transactional(readOnly = true)
    ChatPair validate(Long requesterId, Long receiverId) {
        Map<Long, UserSummaryDto> users = userQueryService.getUserSummaries(List.of(requesterId, receiverId))
                .stream()
                .collect(Collectors.toMap(UserSummaryDto::userId, Function.identity()));
        UserSummaryDto requester = users.get(requesterId);
        if (requester == null || requester.surveyRevision() <= 0) {
            throw new ApiException(ErrorCode.ONBOARDING_REQUIRED);
        }
        if (requesterId.equals(receiverId)) {
            throw new ApiException(ErrorCode.CANNOT_REQUEST_SELF);
        }
        UserSummaryDto receiver = users.get(receiverId);
        if (receiver == null || WITHDRAWN.equals(receiver.status())) {
            throw new ApiException(ErrorCode.TARGET_WITHDRAWN);
        }
        boolean eligible = userQueryService.findCandidates(requesterId).stream()
                .anyMatch(candidate -> candidate.userId().equals(receiverId));
        if (!eligible) {
            throw new ApiException(ErrorCode.TARGET_NOT_ELIGIBLE);
        }
        matchingRepository.findBetween(requesterId, receiverId).ifPresent(ChatRequestValidator::rejectExisting);
        if (matchingRepository.existsConfirmedByParticipant(requesterId)
                || matchingRepository.existsConfirmedByParticipant(receiverId)) {
            throw new ApiException(ErrorCode.ALREADY_MATCHED);
        }
        return new ChatPair(requester, receiver);
    }

    static void rejectExisting(Matching existing) {
        if (existing.isEnded()) {
            throw new ApiException(ErrorCode.MATCHING_ENDED_PAIR);
        }
        throw new ApiException(ErrorCode.ALREADY_CHATTING, Map.of("matching_id", String.valueOf(existing.getId())));
    }

    record ChatPair(UserSummaryDto requester, UserSummaryDto receiver) {
    }
}
