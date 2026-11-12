package kr.bokjakbokjak.recommendation.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Primary
@Profile("test")
public class InMemoryRecommendationSnapshotStore implements RecommendationSnapshotStore {
    private final Map<Long, RecommendationSnapshot> snapshots = new ConcurrentHashMap<>();

    @Override
    public Optional<RecommendationSnapshot> find(Long viewerId) {
        return Optional.ofNullable(snapshots.get(viewerId));
    }

    @Override
    public void save(Long viewerId, RecommendationSnapshot snapshot) {
        snapshots.put(viewerId, snapshot);
    }

    public void clear() {
        snapshots.clear();
    }
}
