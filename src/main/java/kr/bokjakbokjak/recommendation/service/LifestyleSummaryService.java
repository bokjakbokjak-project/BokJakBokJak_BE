package kr.bokjakbokjak.recommendation.service;

import java.time.Instant;
import java.util.List;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import kr.bokjakbokjak.matching.SurveyProfileQueryService;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import kr.bokjakbokjak.recommendation.dto.LifestyleSummaryResponse;
import kr.bokjakbokjak.recommendation.entity.LifestyleSummary;
import kr.bokjakbokjak.recommendation.repository.LifestyleSummaryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class LifestyleSummaryService {
    private final UserQueryService userQueryService;
    private final SurveyProfileQueryService surveyProfileQueryService;
    private final LifestyleSummaryRepository repository;
    private final LifestyleSummaryGenerator generator;
    private final LifestyleSummaryStore store;

    LifestyleSummaryService(UserQueryService userQueryService, SurveyProfileQueryService surveyProfileQueryService,
            LifestyleSummaryRepository repository, LifestyleSummaryGenerator generator, LifestyleSummaryStore store) {
        this.userQueryService = userQueryService;
        this.surveyProfileQueryService = surveyProfileQueryService;
        this.repository = repository;
        this.generator = generator;
        this.store = store;
    }

    public LifestyleSummaryResponse getSummary(Long viewerId, Long userId) {
        checkSurveyCompleted(viewerId, userId);
        SurveyProfileDto profile = loadProfile(userId);
        int revision = profile.surveyRevision();

        var cached = repository.findByUserId(userId).filter(summary -> summary.isFor(revision));
        if (cached.isPresent()) {
            return toResponse(cached.get());
        }

        GeneratedLifestyleSummary generated = generator.generate(profile);
        if (!generated.aiGenerated()) {
            return new LifestyleSummaryResponse(userId, generated.summary(), generated.keywords(), Instant.now());
        }
        return toResponse(saveOrReadConcurrent(userId, revision, generated));
    }

    private void checkSurveyCompleted(Long viewerId, Long userId) {
        UserSummaryDto user = userQueryService.getUserSummaries(List.of(userId)).stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.TARGET_WITHDRAWN));
        if (userId.equals(viewerId)) {
            if (!user.surveyCompleted()) {
                throw new ApiException(ErrorCode.SURVEY_REQUIRED);
            }
            return;
        }
        if (user.surveyRevision() > 0) {
            return;
        }
        throw new ApiException(ErrorCode.TARGET_NOT_ELIGIBLE.status(), ErrorCode.TARGET_NOT_ELIGIBLE.name(),
                "생활습관 요약을 볼 수 없는 사용자입니다.");
    }

    private SurveyProfileDto loadProfile(Long userId) {
        try {
            return surveyProfileQueryService.getSurveyProfile(userId);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.TARGET_WITHDRAWN);
        }
    }

    private LifestyleSummary saveOrReadConcurrent(Long userId, int revision, GeneratedLifestyleSummary generated) {
        try {
            return store.save(userId, revision, generated);
        } catch (DataIntegrityViolationException exception) {
            return repository.findByUserId(userId).orElseThrow(() -> exception);
        }
    }

    private LifestyleSummaryResponse toResponse(LifestyleSummary summary) {
        return new LifestyleSummaryResponse(summary.getUserId(), summary.getSummary(),
                store.readKeywords(summary), summary.getUpdatedAt());
    }
}
