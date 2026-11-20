package kr.bokjakbokjak.user.service;

import java.util.List;
import kr.bokjakbokjak.auth.dto.NextStep;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.entity.MatchingStatus;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import kr.bokjakbokjak.user.dto.MyStatusResponse;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MyStatusService {
    private final UserRepository userRepository;
    private final MatchingRepository matchingRepository;

    public MyStatusService(UserRepository userRepository, MatchingRepository matchingRepository) {
        this.userRepository = userRepository;
        this.matchingRepository = matchingRepository;
    }

    public MyStatusResponse getStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        Long matchingId = matchingRepository
                .findAllByParticipantAndStatusIn(userId, List.of(MatchingStatus.CONFIRMED)).stream()
                .map(Matching::getId)
                .findFirst()
                .orElse(null);
        return new MyStatusResponse(
                user.isOnboardingCompleted(),
                user.isSurveyCompleted(),
                user.isSurveyUpdateRequired(),
                NextStep.of(user),
                matchingId != null,
                matchingId);
    }
}
