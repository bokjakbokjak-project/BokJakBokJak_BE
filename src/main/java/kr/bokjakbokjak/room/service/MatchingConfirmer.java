package kr.bokjakbokjak.room.service;

import java.util.List;
import java.util.stream.Stream;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.notification.entity.NotificationType;
import kr.bokjakbokjak.notification.service.NotificationSender;
import kr.bokjakbokjak.room.dto.MatchingConfirmResponse;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class MatchingConfirmer {
    private static final List<MatchingStatus> CHATTING = List.of(MatchingStatus.CHATTING);

    private final MatchingReader matchingReader;
    private final MatchingRepository matchingRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final NotificationSender notificationSender;
    private final ChatPartnerReader userReader;

    MatchingConfirmer(MatchingReader matchingReader, MatchingRepository matchingRepository,
            ChatMessageRepository chatMessageRepository, NotificationSender notificationSender,
            ChatPartnerReader userReader) {
        this.matchingReader = matchingReader;
        this.matchingRepository = matchingRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.notificationSender = notificationSender;
        this.userReader = userReader;
    }

    @Transactional
    public MatchingConfirmResponse confirm(Long userId, Long matchingId) {
        Matching matching = matchingReader.getParticipating(matchingId, userId);
        if (matching.isEnded()) {
            throw new ApiException(ErrorCode.INVALID_MATCHING_STATUS.status(), ErrorCode.INVALID_MATCHING_STATUS.name(),
                    "중지된 매칭은 확정할 수 없습니다.");
        }
        if (matching.isConfirmed() || matching.isConfirmedBy(userId)) {
            return MatchingConfirmResponse.of(matching, userId);
        }
        Long partnerId = matching.partnerOf(userId);
        if (matchingRepository.existsConfirmedByParticipant(userId)
                || matchingRepository.existsConfirmedByParticipant(partnerId)) {
            throw new ApiException(ErrorCode.ALREADY_MATCHED);
        }

        matching.confirmBy(userId);
        if (matching.isConfirmed()) {
            endOtherChats(matching);
        } else {
            notificationSender.send(partnerId, matchingId, NotificationType.PARTNER_CONFIRMED,
                    MatchingMessages.partnerConfirmedFirst(userReader.read(userId).nickname()));
        }
        matchingRepository.flush();
        return MatchingConfirmResponse.of(matching, userId);
    }

    private void endOtherChats(Matching confirmed) {
        Stream.of(confirmed.getRequesterId(), confirmed.getReceiverId())
                .flatMap(participant -> matchingRepository.findAllByParticipantAndStatusIn(participant, CHATTING)
                        .stream())
                .filter(other -> !other.getId().equals(confirmed.getId()))
                .distinct()
                .forEach(other -> {
                    other.endByPartnerMatched();
                    chatMessageRepository.save(ChatMessage.system(other.getId(),
                            MatchingMessages.partnerMatchedElsewhere(), null));
                });
    }
}
