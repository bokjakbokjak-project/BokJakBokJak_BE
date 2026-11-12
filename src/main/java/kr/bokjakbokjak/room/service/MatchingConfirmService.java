package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.room.dto.MatchingConfirmResponse;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;

@Service
public class MatchingConfirmService {
    private static final int MAX_ATTEMPTS = 3;

    private final MatchingConfirmer matchingConfirmer;
    private final MatchingReader matchingReader;
    private final ResurveyService resurveyService;

    MatchingConfirmService(MatchingConfirmer matchingConfirmer, MatchingReader matchingReader,
            ResurveyService resurveyService) {
        this.matchingConfirmer = matchingConfirmer;
        this.matchingReader = matchingReader;
        this.resurveyService = resurveyService;
    }

    public MatchingConfirmResponse confirm(Long userId, Long matchingId) {
        resurveyService.syncIfStale(matchingReader.getParticipating(matchingId, userId));
        for (int attempt = 1; ; attempt++) {
            try {
                return matchingConfirmer.confirm(userId, matchingId);
            } catch (ConcurrencyFailureException exception) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ApiException(ErrorCode.INVALID_MATCHING_STATUS);
                }
            }
        }
    }
}
