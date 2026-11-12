package kr.bokjakbokjak.room.dto;

import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;

public record MatchingConfirmResponse(Long matchingId, MatchingStatus status, boolean myConfirmed,
        boolean partnerConfirmed) {
    public static MatchingConfirmResponse of(Matching matching, Long userId) {
        return new MatchingConfirmResponse(matching.getId(), matching.getStatus(),
                matching.isConfirmedBy(userId), matching.isConfirmedBy(matching.partnerOf(userId)));
    }
}
