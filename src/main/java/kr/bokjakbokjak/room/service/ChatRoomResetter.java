package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.service.NotificationSender;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.event.AgendasResetEvent;
import kr.bokjakbokjak.room.repository.AgendaRepository;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
class ChatRoomResetter {
    private final MatchingRepository matchingRepository;
    private final AgendaRepository agendaRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationSender notificationSender;
    private final ApplicationEventPublisher eventPublisher;

    ChatRoomResetter(MatchingRepository matchingRepository, AgendaRepository agendaRepository,
            ChatMessageRepository chatMessageRepository, NotificationSender notificationSender,
            ApplicationEventPublisher eventPublisher) {
        this.matchingRepository = matchingRepository;
        this.agendaRepository = agendaRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.notificationSender = notificationSender;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean reset(Long matchingId, Long userId, int revision, String nickname) {
        Matching matching = matchingRepository.findById(matchingId).orElse(null);
        if (matching == null || !matching.applySurveyRevision(userId, revision)) {
            return false;
        }
        agendaRepository.deleteAllByMatchingId(matchingId);
        chatMessageRepository.save(ChatMessage.system(matchingId, MatchingMessages.surveyChanged(nickname), null));
        notificationSender.send(matching.partnerOf(userId), matchingId, NotificationType.AGENDAS_RESET,
                MatchingMessages.agendasResetNotification(nickname));
        eventPublisher.publishEvent(new AgendasResetEvent(matchingId));
        return true;
    }
}
