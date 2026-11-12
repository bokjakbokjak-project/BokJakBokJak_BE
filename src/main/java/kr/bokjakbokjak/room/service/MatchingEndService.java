package kr.bokjakbokjak.room.service;

import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.room.dto.MatchingEndResponse;
import kr.bokjakbokjak.room.entity.ChatMessage;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.ChatMessageRepository;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchingEndService {
    private final MatchingReader matchingReader;
    private final MatchingRepository matchingRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatPartnerReader userReader;

    MatchingEndService(MatchingReader matchingReader, MatchingRepository matchingRepository,
            ChatMessageRepository chatMessageRepository, ChatPartnerReader userReader) {
        this.matchingReader = matchingReader;
        this.matchingRepository = matchingRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userReader = userReader;
    }

    @Transactional
    public MatchingEndResponse end(Long userId, Long matchingId) {
        Matching matching = matchingReader.getParticipating(matchingId, userId);
        if (!matching.isChatting()) {
            throw notChatting();
        }
        matching.endBy(userId);
        try {
            matchingRepository.flush();
        } catch (OptimisticLockingFailureException exception) {
            throw notChatting();
        }
        chatMessageRepository.save(ChatMessage.system(matchingId,
                MatchingMessages.endedBy(userReader.read(userId).nickname()), null));
        return MatchingEndResponse.from(matching);
    }

    private static ApiException notChatting() {
        return new ApiException(ErrorCode.INVALID_MATCHING_STATUS.status(), ErrorCode.INVALID_MATCHING_STATUS.name(),
                "조율 중인 매칭만 중지할 수 있습니다.");
    }
}
