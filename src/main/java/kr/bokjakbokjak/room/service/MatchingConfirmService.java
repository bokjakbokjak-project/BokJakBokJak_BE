package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.room.dto.MatchingConfirmResponse;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class MatchingConfirmService {
    private static final int MAX_ATTEMPTS = 3;

    private final MatchingConfirmer matchingConfirmer;

    MatchingConfirmService(MatchingConfirmer matchingConfirmer) {
        this.matchingConfirmer = matchingConfirmer;
    }

    public MatchingConfirmResponse confirm(Long userId, Long matchingId) {
        for (int attempt = 1; ; attempt++) {
            try {
                return matchingConfirmer.confirm(userId, matchingId);
            } catch (OptimisticLockingFailureException exception) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ApiException(ErrorCode.INVALID_MATCHING_STATUS);
                }
            }
        }
    }
}
