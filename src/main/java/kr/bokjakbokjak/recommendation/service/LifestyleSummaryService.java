package kr.bokjakbokjak.recommendation.service;

import java.time.Instant;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.matching.SurveyProfileDto;
import kr.bokjakbokjak.matching.SurveyProfileQueryService;
import kr.bokjakbokjak.recommendation.dto.LifestyleSummaryResponse;
import kr.bokjakbokjak.recommendation.entity.LifestyleSummary;
import kr.bokjakbokjak.recommendation.repository.LifestyleSummaryRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class LifestyleSummaryService {
    private final SurveyProfileQueryService surveyProfileQueryService;
    private final LifestyleSummaryRepository repository;
    private final LifestyleSummaryGenerator generator;
    private final LifestyleSummaryStore store;

    LifestyleSummaryService(SurveyProfileQueryService surveyProfileQueryService,
            LifestyleSummaryRepository repository, LifestyleSummaryGenerator generator, LifestyleSummaryStore store) {
        this.surveyProfileQueryService = surveyProfileQueryService;
        this.repository = repository;
        this.generator = generator;
        this.store = store;
    }

    public LifestyleSummaryResponse getSummary(Long userId) {
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
