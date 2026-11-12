package kr.bokjakbokjak.recommendation.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

record RecommendationSnapshot(int viewerRevision, Map<Long, ScoredCandidate> scores,
        List<RankedCandidate> ranking) {

    RecommendationSnapshot {
        scores = Map.copyOf(scores);
        ranking = List.copyOf(ranking);
    }

    Optional<ScoredCandidate> freshScore(int currentViewerRevision, Long candidateId, int candidateRevision) {
        if (viewerRevision != currentViewerRevision) {
            return Optional.empty();
        }
        return Optional.ofNullable(scores.get(candidateId))
                .filter(cached -> cached.surveyRevision() == candidateRevision);
    }

    record ScoredCandidate(int surveyRevision, Integer matchScore) {
    }
}
