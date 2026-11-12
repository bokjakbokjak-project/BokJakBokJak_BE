package kr.bokjakbokjak.room.service;

import java.util.List;
import java.util.stream.IntStream;
import kr.bokjakbokjak.room.entity.Agenda;
import kr.bokjakbokjak.room.entity.AgendaStatus;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class AgendaCreator {
    private final MatchingRepository matchingRepository;
    private final AgendaRepository agendaRepository;
    private final ChatMessageRepository chatMessageRepository;

    AgendaCreator(MatchingRepository matchingRepository, AgendaRepository agendaRepository,
            ChatMessageRepository chatMessageRepository) {
        this.matchingRepository = matchingRepository;
        this.agendaRepository = agendaRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public boolean create(Long matchingId, List<GeneratedAgenda> generated) {
        boolean chatting = matchingRepository.findById(matchingId).map(Matching::isChatting).orElse(false);
        if (!chatting || generated.isEmpty() || agendaRepository.existsByMatchingId(matchingId)) {
            return false;
        }
        List<Agenda> agendas = agendaRepository.saveAll(IntStream.range(0, generated.size())
                .mapToObj(i -> toAgenda(matchingId, i + 1, generated.get(i)))
                .toList());
        Agenda first = agendas.getFirst();
        chatMessageRepository.save(ChatMessage.ai(matchingId,
                AgendaMessages.introduce(first.getSeq(), first.getContent()), first.getId()));
        return true;
    }

    private static Agenda toAgenda(Long matchingId, int seq, GeneratedAgenda generated) {
        AgendaStatus status = seq == 1 ? AgendaStatus.DISCUSSING : AgendaStatus.WAITING;
        return Agenda.create(matchingId, seq, generated.topicCode(), generated.title(), generated.content(), status);
    }
}
