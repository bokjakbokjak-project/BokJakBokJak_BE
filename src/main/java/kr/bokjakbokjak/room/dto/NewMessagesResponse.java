package kr.bokjakbokjak.room.dto;

import java.util.List;

public record NewMessagesResponse(List<MessageResponse> messages) {
    public NewMessagesResponse {
        messages = List.copyOf(messages);
    }
}
