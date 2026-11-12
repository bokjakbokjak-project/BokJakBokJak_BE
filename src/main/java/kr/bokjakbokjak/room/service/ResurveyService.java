package kr.bokjakbokjak.room.service;

import java.util.List;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;

@Service
public class ResurveyService {
    private static final Logger log = LoggerFactory.getLogger(ResurveyService.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final List<MatchingStatus> CHATTING = List.of(MatchingStatus.CHATTING);

    private final MatchingRepository matchingRepository;
    private final ChatRoomResetter resetter;
    private final ChatPartnerReader userReader;

    ResurveyService(MatchingRepository matchingRepository, ChatRoomResetter resetter, ChatPartnerReader userReader) {
        this.matchingRepository = matchingRepository;
        this.resetter = resetter;
        this.userReader = userReader;
    }

    public int resetChattingRooms(Long userId, int revision) {
        List<Matching> chatting = matchingRepository.findAllByParticipantAndStatusIn(userId, CHATTING);
        if (chatting.isEmpty()) {
            return 0;
        }
        String nickname = userReader.read(userId).nickname();
        int reset = 0;
        for (Matching matching : chatting) {
            if (resetWithRetry(matching.getId(), userId, revision, nickname)) {
                reset++;
            }
        }
        return reset;
    }

    boolean resetWithRetry(Long matchingId, Long userId, int revision, String nickname) {
        for (int attempt = 1; ; attempt++) {
            try {
                return resetter.reset(matchingId, userId, revision, nickname);
            } catch (ConcurrencyFailureException exception) {
                if (attempt >= MAX_ATTEMPTS) {
                    log.warn("Chat room reset gave up: matching={}, user={}", matchingId, userId);
                    return false;
                }
            }
        }
    }
}
