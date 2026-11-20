package kr.bokjakbokjak.room.service;

import java.util.List;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MessageType;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.room.service.RuleSummaryPrompt.ChatLine;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

@Component
class RuleRecorder {
    private static final int MAX_MESSAGES = 60;

    private final AgendaRepository agendaRepository;
    private final MatchingRepository matchingRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RuleSummaryGenerator generator;

    RuleRecorder(AgendaRepository agendaRepository, MatchingRepository matchingRepository,
            ChatMessageRepository chatMessageRepository, RuleSummaryGenerator generator) {
        this.agendaRepository = agendaRepository;
        this.matchingRepository = matchingRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.generator = generator;
    }

    void record(Long matchingId, Long agendaId) {
        Agenda agenda = agendaRepository.findByIdAndMatchingId(agendaId, matchingId).orElse(null);
        Matching matching = matchingRepository.findById(matchingId).orElse(null);
        if (agenda == null || matching == null || !agenda.isDecided() || agenda.getRuleSummary() != null) {
            return;
        }
        List<ChatLine> lines = chatMessageRepository
                .findAllByAgendaIdAndTypeOrderByIdDesc(agendaId, MessageType.USER, Limit.of(MAX_MESSAGES))
                .reversed()
                .stream()
                .map(message -> new ChatLine(speakerOf(matching, message), message.getContent()))
                .toList();
        generator.summarize(agenda.getTitle(), agenda.getContent(), lines)
                .ifPresent(rule -> agendaRepository.recordRuleIfEmpty(agendaId, rule));
    }

    private static String speakerOf(Matching matching, ChatMessage message) {
        return matching.getRequesterId().equals(message.getSenderId()) ? "A" : "B";
    }
}
