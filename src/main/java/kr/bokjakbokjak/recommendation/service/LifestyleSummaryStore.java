package kr.bokjakbokjak.recommendation.service;

import java.util.List;
import kr.bokjakbokjak.recommendation.entity.LifestyleSummary;
import kr.bokjakbokjak.recommendation.repository.LifestyleSummaryRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
class LifestyleSummaryStore {
    private final LifestyleSummaryRepository repository;
    private final ObjectMapper objectMapper;

    LifestyleSummaryStore(LifestyleSummaryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    LifestyleSummary save(Long userId, int surveyRevision, GeneratedLifestyleSummary generated) {
        String keywords = objectMapper.writeValueAsString(generated.keywords());
        return repository.findByUserId(userId)
                .map(existing -> {
                    existing.refresh(surveyRevision, generated.summary(), keywords);
                    return existing;
                })
                .orElseGet(() -> repository.saveAndFlush(
                        LifestyleSummary.create(userId, surveyRevision, generated.summary(), keywords)));
    }

    List<String> readKeywords(LifestyleSummary summary) {
        return List.of(objectMapper.readValue(summary.getKeywords(), String[].class));
    }
}
