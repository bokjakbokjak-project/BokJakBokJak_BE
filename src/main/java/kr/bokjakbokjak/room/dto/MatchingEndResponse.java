package kr.bokjakbokjak.room.dto;

import java.time.Instant;
import kr.bokjakbokjak.room.entity.EndReason;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;

public record MatchingEndResponse(Long matchingId, MatchingStatus status, EndReason endReason, Instant endedAt) {
    public static MatchingEndResponse from(Matching matching) {
        return new MatchingEndResponse(matching.getId(), matching.getStatus(), matching.getEndReason(),
                matching.getEndedAt());
    }
}
