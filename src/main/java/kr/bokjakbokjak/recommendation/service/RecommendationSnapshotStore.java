package kr.bokjakbokjak.recommendation.service;

import java.util.Optional;

interface RecommendationSnapshotStore {
    Optional<RecommendationSnapshot> find(Long viewerId);

    void save(Long viewerId, RecommendationSnapshot snapshot);
}
