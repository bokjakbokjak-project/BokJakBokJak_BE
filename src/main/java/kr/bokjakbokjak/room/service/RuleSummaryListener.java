package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.room.event.AgendaDecidedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnBooleanProperty(name = "app.agenda.auto-summarize", matchIfMissing = true)
class RuleSummaryListener {
    private static final Logger log = LoggerFactory.getLogger(RuleSummaryListener.class);

    private final RuleRecorder ruleRecorder;

    RuleSummaryListener(RuleRecorder ruleRecorder) {
        this.ruleRecorder = ruleRecorder;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendaDecided(AgendaDecidedEvent event) {
        try {
            ruleRecorder.record(event.matchingId(), event.agendaId());
        } catch (RuntimeException exception) {
            log.error("Rule summary failed for agenda {}", event.agendaId(), exception);
        }
    }
}
