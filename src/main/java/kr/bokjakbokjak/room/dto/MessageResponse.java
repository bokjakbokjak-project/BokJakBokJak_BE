package kr.bokjakbokjak.room.dto;

import java.time.Instant;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.MessageType;

public record MessageResponse(
        Long messageId,
        MessageType type,
        Long senderId,
        String content,
        Long agendaId,
        Instant createdAt) {

    public static MessageResponse from(ChatMessage message) {
        return new MessageResponse(
                message.getId(),
                message.getType(),
                message.getSenderId(),
                message.getContent(),
                message.getAgendaId(),
                message.getCreatedAt());
    }
}
