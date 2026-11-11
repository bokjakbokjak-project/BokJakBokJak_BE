package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MatchingReader {
    private final MatchingRepository matchingRepository;

    public MatchingReader(MatchingRepository matchingRepository) {
        this.matchingRepository = matchingRepository;
    }

    @Transactional(readOnly = true)
    public Matching getParticipating(Long matchingId, Long userId) {
        Matching matching = matchingRepository.findById(matchingId)
                .orElseThrow(() -> new ApiException(ErrorCode.MATCHING_NOT_FOUND));
        if (!matching.isParticipant(userId)) {
            throw new ApiException(ErrorCode.MATCHING_FORBIDDEN);
        }
        return matching;
    }
}
