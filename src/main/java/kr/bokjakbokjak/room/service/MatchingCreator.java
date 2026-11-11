package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.service.NotificationSender;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.event.ChatStartedEvent;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class MatchingCreator {
    private static final String CHAT_REQUESTED_MESSAGE = "%s님과의 매칭이 시작되었어요";

    private final MatchingRepository matchingRepository;
    private final NotificationSender notificationSender;
    private final ApplicationEventPublisher eventPublisher;

    MatchingCreator(MatchingRepository matchingRepository, NotificationSender notificationSender,
            ApplicationEventPublisher eventPublisher) {
        this.matchingRepository = matchingRepository;
        this.notificationSender = notificationSender;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    Matching create(ChatRequestValidator.ChatPair pair) {
        Matching matching = matchingRepository.saveAndFlush(Matching.start(
                pair.requester().userId(), pair.receiver().userId(),
                pair.requester().surveyRevision(), pair.receiver().surveyRevision()));
        notificationSender.send(pair.receiver().userId(), matching.getId(), NotificationType.CHAT_REQUESTED,
                CHAT_REQUESTED_MESSAGE.formatted(pair.requester().nickname()));
        eventPublisher.publishEvent(new ChatStartedEvent(matching.getId()));
        return matching;
    }
}
