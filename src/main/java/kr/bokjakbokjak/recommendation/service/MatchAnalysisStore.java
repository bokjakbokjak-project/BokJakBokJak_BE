package kr.bokjakbokjak.recommendation.service;

import java.util.List;
import java.util.Optional;
import kr.bokjakbokjak.recommendation.entity.AiAnalysis;
import kr.bokjakbokjak.recommendation.repository.AiAnalysisRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
class MatchAnalysisStore {
    private final AiAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    MatchAnalysisStore(AiAnalysisRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<AiAnalysis> findFresh(Long viewerId, Long targetId, int viewerRevision, int targetRevision) {
        return repository.findByViewerIdAndTargetId(viewerId, targetId)
                .filter(analysis -> analysis.isFor(viewerRevision, targetRevision));
    }

    @Transactional
    public AiAnalysis save(Long viewerId, Long targetId, int viewerRevision, int targetRevision,
            GeneratedMatchAnalysis generated) {
        String good = toJson(generated.goodPoints());
        String adjust = toJson(generated.adjustPoints());
        String check = toJson(generated.checkPoints());
        return repository.findByViewerIdAndTargetId(viewerId, targetId)
                .map(existing -> {
                    existing.refresh(viewerRevision, targetRevision, good, adjust, check);
                    return existing;
                })
                .orElseGet(() -> repository.saveAndFlush(
                        AiAnalysis.create(viewerId, targetId, viewerRevision, targetRevision, good, adjust, check)));
    }

    List<String> read(String json) {
        return List.of(objectMapper.readValue(json, String[].class));
    }

    private String toJson(List<String> sentences) {
        return objectMapper.writeValueAsString(sentences);
    }
}
