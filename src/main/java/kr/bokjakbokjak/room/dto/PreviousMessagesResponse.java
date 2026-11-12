package kr.bokjakbokjak.room.dto;

import java.util.List;

public record PreviousMessagesResponse(List<MessageResponse> messages, boolean hasMore) {
    public PreviousMessagesResponse {
        messages = List.copyOf(messages);
    }
}
