package kr.bokjakbokjak.recommendation.service;

import java.util.Comparator;
import java.util.List;

record RankedCandidate(Long userId, Integer matchScore) {
    static final Comparator<RankedCandidate> ORDER = Comparator
            .comparing(RankedCandidate::matchScore, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(RankedCandidate::userId);

    static List<RankedCandidate> rank(List<RankedCandidate> candidates) {
        return candidates.stream().sorted(ORDER).toList();
    }
}
