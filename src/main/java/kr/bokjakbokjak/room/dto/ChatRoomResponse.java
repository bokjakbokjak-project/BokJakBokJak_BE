package kr.bokjakbokjak.room.dto;

import java.util.List;
import kr.bokjakbokjak.matching.UserSummaryDto;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.EndReason;
import kr.bokjakbokjak.room.entity.MatchingStatus;

public record ChatRoomResponse(
        Long matchingId,
        MatchingStatus status,
        EndReason endReason,
        Partner partner,
        List<AgendaItem> agendas,
        boolean myConfirmed,
        boolean partnerConfirmed) {

    public ChatRoomResponse {
        agendas = List.copyOf(agendas);
    }

    public record Partner(Long userId, String nickname, String department, List<String> topCategories) {
        public Partner {
            topCategories = List.copyOf(topCategories);
        }

        public static Partner from(UserSummaryDto user) {
            return new Partner(user.userId(), user.nickname(), user.department(), user.topCategories());
        }
    }

    public record AgendaItem(Long agendaId, int seq, String title, String content, AgendaStatus status,
            String ruleSummary) {
        public static AgendaItem from(Agenda agenda) {
            return new AgendaItem(agenda.getId(), agenda.getSeq(), agenda.getTitle(), agenda.getContent(),
                    agenda.getStatus(), agenda.getRuleSummary());
        }
    }
}
