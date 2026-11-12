package kr.bokjakbokjak.room.service;

import java.util.List;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.event.ChatStartedEvent;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class AgendaPreparationListener {
    private static final Logger log = LoggerFactory.getLogger(AgendaPreparationListener.class);

    private final MatchingRepository matchingRepository;
    private final AgendaGenerator agendaGenerator;
    private final AgendaCreator agendaCreator;

    AgendaPreparationListener(MatchingRepository matchingRepository, AgendaGenerator agendaGenerator,
            AgendaCreator agendaCreator) {
        this.matchingRepository = matchingRepository;
        this.agendaGenerator = agendaGenerator;
        this.agendaCreator = agendaCreator;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChatStarted(ChatStartedEvent event) {
        prepare(event.matchingId());
    }

    void prepare(Long matchingId) {
        try {
            Matching matching = matchingRepository.findById(matchingId).orElse(null);
            if (matching == null || !matching.isChatting()) {
                return;
            }
            List<GeneratedAgenda> agendas = agendaGenerator.generate(matching.getRequesterId(), matching.getReceiverId());
            agendaCreator.create(matchingId, agendas);
        } catch (RuntimeException exception) {
            log.error("Agenda preparation failed for matching {}", matchingId, exception);
        }
    }
}
