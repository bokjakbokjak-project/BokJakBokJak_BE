package kr.bokjakbokjak.recommendation.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import kr.bokjakbokjak.common.ApiException;
import kr.bokjakbokjak.common.ErrorCode;
import kr.bokjakbokjak.matching.CandidateDto;
import kr.bokjakbokjak.matching.UserQueryService;
import kr.bokjakbokjak.matching.UserSummaryDto;
import kr.bokjakbokjak.recommendation.dto.RecommendationItem;
import kr.bokjakbokjak.recommendation.dto.RecommendationListResponse;
import kr.bokjakbokjak.recommendation.dto.RecommendationRefreshResponse;
import kr.bokjakbokjak.recommendation.dto.RecommendationRelation;
import kr.bokjakbokjak.room.entity.Matching;
import kr.bokjakbokjak.room.repository.MatchingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationService {
    private final UserQueryService userQueryService;
    private final MatchingRepository matchingRepository;
    private final RecommendationCalculator calculator;

    RecommendationService(UserQueryService userQueryService, MatchingRepository matchingRepository,
            RecommendationCalculator calculator) {
        this.userQueryService = userQueryService;
        this.matchingRepository = matchingRepository;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public RecommendationListResponse getRecommendations(Long viewerId) {
        return RecommendationListResponse.of(recommend(viewerId, false).items());
    }

    @Transactional(readOnly = true)
    public RecommendationRefreshResponse refresh(Long viewerId) {
        Recommendations result = recommend(viewerId, true);
        return RecommendationRefreshResponse.of(result.changed(), result.items());
    }

    private Recommendations recommend(Long viewerId, boolean recalculateAll) {
        Map<Long, CandidateDto> candidates = eligibleCandidates(viewerId);
        Map<Long, UserSummaryDto> users = userQueryService.getUserSummaries(
                        Stream.concat(Stream.of(viewerId), candidates.keySet().stream()).toList())
                .stream()
                .collect(Collectors.toMap(UserSummaryDto::userId, Function.identity(), (first, second) -> first));
        UserSummaryDto viewer = users.get(viewerId);
        if (viewer == null || viewer.surveyRevision() <= 0) {
            throw new ApiException(ErrorCode.ONBOARDING_REQUIRED);
        }

        Map<Long, Integer> revisions = new LinkedHashMap<>();
        candidates.keySet().forEach(id -> revisions.put(id,
                users.containsKey(id) ? users.get(id).surveyRevision() : 0));
        RecommendationCalculator.Result result = calculator.calculate(viewerId, viewer.surveyRevision(), revisions,
                recalculateAll);

        Map<Long, RecommendationRelation> relations = relationsOf(viewerId);
        List<RecommendationItem> items = result.ranking().stream()
                .map(ranked -> toItem(candidates.get(ranked.userId()), ranked.matchScore(),
                        relations.getOrDefault(ranked.userId(), RecommendationRelation.NONE)))
                .toList();
        return new Recommendations(items, result.changed());
    }

    private Map<Long, CandidateDto> eligibleCandidates(Long viewerId) {
        List<CandidateDto> found = userQueryService.findCandidates(viewerId).stream()
                .filter(candidate -> !candidate.userId().equals(viewerId))
                .toList();
        if (found.isEmpty()) {
            return Map.of();
        }
        Set<Long> matched = matchingRepository.findAllConfirmedByParticipantIn(
                        found.stream().map(CandidateDto::userId).toList())
                .stream()
                .flatMap(matching -> Stream.of(matching.getRequesterId(), matching.getReceiverId()))
                .collect(Collectors.toSet());
        return found.stream()
                .filter(candidate -> !matched.contains(candidate.userId()))
                .collect(Collectors.toMap(CandidateDto::userId, Function.identity(), (first, second) -> first,
                        LinkedHashMap::new));
    }

    private Map<Long, RecommendationRelation> relationsOf(Long viewerId) {
        return matchingRepository.findAllByParticipant(viewerId).stream()
                .collect(Collectors.toMap(matching -> matching.partnerOf(viewerId), RecommendationService::relationOf));
    }

    private static RecommendationRelation relationOf(Matching matching) {
        return matching.isEnded() ? RecommendationRelation.ENDED : RecommendationRelation.CHATTING;
    }

    private static RecommendationItem toItem(CandidateDto candidate, Integer matchScore,
            RecommendationRelation relation) {
        return new RecommendationItem(candidate.userId(), candidate.nickname(), candidate.department(), null,
                candidate.topCategories(), matchScore, relation);
    }

    private record Recommendations(List<RecommendationItem> items, boolean changed) {
    }
}
