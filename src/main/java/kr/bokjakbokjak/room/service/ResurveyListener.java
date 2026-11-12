package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.common.SurveyRevisionChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ResurveyListener {
    private static final Logger log = LoggerFactory.getLogger(ResurveyListener.class);

    private final ResurveyService resurveyService;

    ResurveyListener(ResurveyService resurveyService) {
        this.resurveyService = resurveyService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSurveyRevisionChanged(SurveyRevisionChangedEvent event) {
        try {
            resurveyService.resetChattingRooms(event.userId(), event.revision());
        } catch (RuntimeException exception) {
            log.error("Resurvey handling failed for user {}", event.userId(), exception);
        }
    }
}
