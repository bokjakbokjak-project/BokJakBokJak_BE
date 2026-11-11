package kr.bokjakbokjak.recommendation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.recommendation.dto.LifestyleSummaryResponse;
import kr.bokjakbokjak.recommendation.service.LifestyleSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "생활습관 요약")
@RestController
public class LifestyleSummaryController {
    private final LifestyleSummaryService lifestyleSummaryService;

    public LifestyleSummaryController(LifestyleSummaryService lifestyleSummaryService) {
        this.lifestyleSummaryService = lifestyleSummaryService;
    }

    @Operation(summary = "개인 생활습관 AI 요약 조회",
            description = "설문이 바뀌지 않았으면 저장된 요약을 반환하고, 처음이거나 바뀌었으면 새로 생성한다.")
    @GetMapping("/api/users/{userId}/lifestyle-summary")
    public ApiResponse.Success<LifestyleSummaryResponse> getSummary(@PathVariable Long userId) {
        return ApiResponse.success(SuccessCode.LIFESTYLE_SUMMARY_SUCCESS, lifestyleSummaryService.getSummary(userId));
    }
}
