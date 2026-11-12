package kr.bokjakbokjak.room.dto;

import java.time.Instant;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;

public record ChatRequestResponse(Long matchingId, MatchingStatus status, Instant createdAt) {
    public static ChatRequestResponse from(Matching matching) {
        return new ChatRequestResponse(matching.getId(), matching.getStatus(), matching.getCreatedAt());
    }
}
