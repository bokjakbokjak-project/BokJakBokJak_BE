package kr.bokjakbokjak.room.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.room.dto.AgendaDecideResponse;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendaService {
    private final MatchingReader matchingReader;
    private final AgendaRepository agendaRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPartnerReader userReader;

    AgendaService(MatchingReader matchingReader, AgendaRepository agendaRepository,
            ChatMessageRepository chatMessageRepository, ChatPartnerReader userReader) {
        this.matchingReader = matchingReader;
        this.agendaRepository = agendaRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userReader = userReader;
    }

    @Transactional
    public AgendaDecideResponse decide(Long userId, Long matchingId, Long agendaId) {
        Matching matching = matchingReader.getParticipating(matchingId, userId);
        if (!matching.isChatting()) {
            throw new ApiException(ErrorCode.INVALID_MATCHING_STATUS.status(), ErrorCode.INVALID_MATCHING_STATUS.name(),
                    "조율 중인 채팅방에서만 안건을 확정할 수 있습니다.");
        }
        Agenda agenda = agendaRepository.findByIdAndMatchingId(agendaId, matchingId)
                .orElseThrow(() -> new ApiException(ErrorCode.AGENDA_NOT_FOUND));
        if (agenda.isWaiting()) {
            throw new ApiException(ErrorCode.AGENDA_NOT_STARTED);
        }
        if (!agenda.isDecided() && agendaRepository.decideIfDiscussing(agendaId, userId, Instant.now()) == 1) {
            List<Agenda> agendas = agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId);
            Agenda decided = find(agendas, agendaId);
            chatMessageRepository.save(ChatMessage.system(matchingId,
                    AgendaMessages.decided(userReader.read(userId).nickname()), agendaId));
            nextOf(agendas, decided).ifPresent(next -> startNext(matchingId, next));
            return toResponse(decided, agendas);
        }
        List<Agenda> agendas = agendaRepository.findAllByMatchingIdOrderBySeqAsc(matchingId);
        return toResponse(find(agendas, agendaId), agendas);
    }

    private void startNext(Long matchingId, Agenda next) {
        next.start();
        chatMessageRepository.save(ChatMessage.ai(matchingId,
                AgendaMessages.introduce(next.getSeq(), next.getContent()), next.getId()));
    }

    private static Agenda find(List<Agenda> agendas, Long agendaId) {
        return agendas.stream()
                .filter(agenda -> agenda.getId().equals(agendaId))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.AGENDA_NOT_FOUND));
    }

    private static Optional<Agenda> nextOf(List<Agenda> agendas, Agenda current) {
        return agendas.stream().filter(agenda -> agenda.getSeq() > current.getSeq()).findFirst();
    }

    private static AgendaDecideResponse toResponse(Agenda decided, List<Agenda> agendas) {
        return new AgendaDecideResponse(
                decided.getId(),
                decided.getStatus(),
                decided.getDecidedBy(),
                nextOf(agendas, decided).map(Agenda::getId).orElse(null),
                agendas.stream().allMatch(Agenda::isDecided));
    }
}
