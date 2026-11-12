package kr.bokjakbokjak.recommendation.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import kr.bokjakbokjak.recommendation.service.RecommendationSnapshot.ScoredCandidate;
import org.springframework.stereotype.Component;

@Component
class RecommendationCalculator {
    private final RecommendationScorer scorer;
    private final RecommendationSnapshotStore store;

    RecommendationCalculator(RecommendationScorer scorer, RecommendationSnapshotStore store) {
        this.scorer = scorer;
        this.store = store;
    }

    Result calculate(Long viewerId, int viewerRevision, Map<Long, Integer> candidateRevisions, boolean recalculateAll) {
        Optional<RecommendationSnapshot> previous = store.find(viewerId);
        Map<Long, ScoredCandidate> scores = new HashMap<>();
        candidateRevisions.forEach((candidateId, revision) -> scores.put(candidateId, previous
                .filter(snapshot -> !recalculateAll)
                .flatMap(snapshot -> snapshot.freshScore(viewerRevision, candidateId, revision))
                .orElseGet(() -> new ScoredCandidate(revision, scorer.score(viewerId, candidateId)))));

        List<RankedCandidate> ranking = RankedCandidate.rank(scores.entrySet().stream()
                .map(entry -> new RankedCandidate(entry.getKey(), entry.getValue().matchScore()))
                .toList());
        RecommendationSnapshot current = new RecommendationSnapshot(viewerRevision, scores, ranking);
        if (!previous.map(current::equals).orElse(false)) {
            store.save(viewerId, current);
        }
        boolean changed = previous.map(snapshot -> !snapshot.ranking().equals(ranking)).orElse(true);
        return new Result(ranking, changed);
    }

    record Result(List<RankedCandidate> ranking, boolean changed) {
    }
}
