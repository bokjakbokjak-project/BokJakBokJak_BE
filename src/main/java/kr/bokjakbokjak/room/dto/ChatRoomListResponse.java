package kr.bokjakbokjak.room.dto;

import java.time.Instant;
import java.util.List;
import kr.bokjakbokjak.room.entity.MatchingStatus;

public record ChatRoomListResponse(List<Room> rooms) {
    public ChatRoomListResponse {
        rooms = List.copyOf(rooms);
    }

    public record Room(Long matchingId, Partner partner, MatchingStatus status, String lastMessage,
            Instant lastMessageAt) {
    }

    public record Partner(Long userId, String nickname, List<String> topCategories) {
        public Partner {
            topCategories = List.copyOf(topCategories);
        }
    }
}
