package kr.bokjakbokjak.room.dto;

import kr.bokjakbokjak.room.entity.AgendaStatus;

public record AgendaDecideResponse(Long agendaId, AgendaStatus status, Long decidedBy, Long nextAgendaId,
        boolean allDecided) {
}
