package kr.bokjakbokjak.recommendation.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.matching.MatchResultDto;
import kr.bokjakbokjak.matching.MatchingEngine;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import kr.bokjakbokjak.recommendation.dto.MatchAnalysisResponse;
import kr.bokjakbokjak.recommendation.entity.AiAnalysis;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class MatchAnalysisService {
    private static final String ACTIVE = "ACTIVE";

    private final UserQueryService userQueryService;
    private final MatchingEngine matchingEngine;
    private final MatchAnalysisGenerator generator;
    private final MatchAnalysisStore store;

    MatchAnalysisService(UserQueryService userQueryService, MatchingEngine matchingEngine,
            MatchAnalysisGenerator generator, MatchAnalysisStore store) {
        this.userQueryService = userQueryService;
        this.matchingEngine = matchingEngine;
        this.generator = generator;
        this.store = store;
    }

    public MatchAnalysisResponse getAnalysis(Long viewerId, Long targetId) {
        Map<Long, UserSummaryDto> users = userQueryService.getUserSummaries(List.of(viewerId, targetId)).stream()
                .collect(Collectors.toMap(UserSummaryDto::userId, Function.identity(), (first, second) -> first));
        UserSummaryDto viewer = users.get(viewerId);
        if (viewer == null || viewer.surveyRevision() <= 0) {
            throw new ApiException(ErrorCode.ONBOARDING_REQUIRED);
        }
        UserSummaryDto target = users.get(targetId);
        if (target == null || !ACTIVE.equals(target.status())) {
            throw new ApiException(ErrorCode.TARGET_WITHDRAWN);
        }
        if (viewerId.equals(targetId) || target.surveyRevision() <= 0) {
            throw notAnalyzable();
        }

        MatchResultDto match = calculate(viewerId, targetId);
        int viewerRevision = viewer.surveyRevision();
        int targetRevision = target.surveyRevision();
        MatchAnalysisResponse.Target card = new MatchAnalysisResponse.Target(target.userId(), target.nickname(),
                KoreanAge.of(target.birthYear()), target.department(), target.topCategories());
        Integer score = RecommendationScorer.toPercent(match.mutualScore());

        var cached = store.findFresh(viewerId, targetId, viewerRevision, targetRevision);
        if (cached.isPresent()) {
            return fromSaved(card, score, cached.get());
        }
        GeneratedMatchAnalysis generated = generator.generate(match.items());
        if (!generated.aiGenerated()) {
            return new MatchAnalysisResponse(card, score, generated.goodPoints(), generated.adjustPoints(),
                    generated.checkPoints(), Instant.now());
        }
        return fromSaved(card, score, saveOrReadConcurrent(viewerId, targetId, viewerRevision, targetRevision,
                generated));
    }

    private MatchResultDto calculate(Long viewerId, Long targetId) {
        try {
            return matchingEngine.calculate(viewerId, targetId);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw notAnalyzable();
        }
    }

    private AiAnalysis saveOrReadConcurrent(Long viewerId, Long targetId, int viewerRevision, int targetRevision,
            GeneratedMatchAnalysis generated) {
        try {
            return store.save(viewerId, targetId, viewerRevision, targetRevision, generated);
        } catch (DataIntegrityViolationException exception) {
            return store.findFresh(viewerId, targetId, viewerRevision, targetRevision).orElseThrow(() -> exception);
        }
    }

    private MatchAnalysisResponse fromSaved(MatchAnalysisResponse.Target card, Integer score, AiAnalysis analysis) {
        return new MatchAnalysisResponse(card, score, store.read(analysis.getGoodPoints()),
                store.read(analysis.getAdjustPoints()), store.read(analysis.getCheckPoints()),
                analysis.getUpdatedAt());
    }

    private static ApiException notAnalyzable() {
        return new ApiException(ErrorCode.TARGET_NOT_ELIGIBLE.status(), ErrorCode.TARGET_NOT_ELIGIBLE.name(),
                "매칭 분석을 할 수 없는 사용자입니다.");
    }
}
