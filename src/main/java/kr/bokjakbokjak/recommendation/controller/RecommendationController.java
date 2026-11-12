package kr.bokjakbokjak.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.recommendation.dto.RecommendationListResponse;
import kr.bokjakbokjak.recommendation.dto.RecommendationRefreshResponse;
import kr.bokjakbokjak.recommendation.service.RecommendationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천")
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @Operation(summary = "추천 리스트 조회", description = "후보 전체를 적합도 순으로 한 번에 반환한다. 점수를 낼 수 없는 후보는 맨 뒤에 둔다.")
    @GetMapping
    public ApiResponse.Success<RecommendationListResponse> getRecommendations(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessCode.RECOMMENDATION_LIST_SUCCESS,
                recommendationService.getRecommendations(userId));
    }

    @Operation(summary = "추천 새로고침", description = "후보 전체를 다시 계산하고, 직전 결과와 순서·점수가 다르면 changed가 true다.")
    @PostMapping("/refresh")
    public ApiResponse.Success<RecommendationRefreshResponse> refresh(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessCode.RECOMMENDATION_REFRESH_SUCCESS, recommendationService.refresh(userId));
    }
}
