package kr.bokjakbokjak.share;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.user.entity.User;
import kr.bokjakbokjak.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공유 링크를 받은 사람과 공유한 사람의 매칭률.
 * 친구끼리 재미로 보는 기능이라 추천과 달리 학교·기숙사·성별·최종 매칭 여부를 보지 않는다.
 */
@Service
@Transactional(readOnly = true)
public class ShareMatchService {
    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    private final UserRepository userRepository;
    private final MatchingEngine matchingEngine;

    public ShareMatchService(UserRepository userRepository, MatchingEngine matchingEngine) {
        this.userRepository = userRepository;
        this.matchingEngine = matchingEngine;
    }

    public ShareMatchScoreResponse getMatchScore(Long viewerId, Long targetId) {
        if (viewerId.equals(targetId)) {
            throw new ApiException(ErrorCode.INVALID_INPUT, Map.of("user_id", "본인과의 매칭률은 볼 수 없습니다."));
        }
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (!viewer.isSurveyCompleted()) {
            throw new ApiException(ErrorCode.SURVEY_REQUIRED);
        }
        // 상대는 설문 개편 뒤 재설문 전이어도 예전 답으로 계산한다
        userRepository.findById(targetId)
                .filter(User::isActive)
                .filter(User::hasSubmittedSurvey)
                .orElseThrow(() -> new ApiException(ErrorCode.TARGET_WITHDRAWN));

        BigDecimal ratio = matchingEngine.calculate(viewerId, targetId).mutualScore();
        return new ShareMatchScoreResponse(
                targetId, ratio.multiply(PERCENT).setScale(0, RoundingMode.HALF_UP).intValueExact());
    }
}
