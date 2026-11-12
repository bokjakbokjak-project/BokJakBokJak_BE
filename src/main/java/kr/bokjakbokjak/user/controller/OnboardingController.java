package kr.bokjakbokjak.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.user.dto.OnboardingRequest;
import kr.bokjakbokjak.user.dto.OnboardingResponse;
import kr.bokjakbokjak.user.service.OnboardingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "온보딩")
@RestController
@RequestMapping("/api/me/onboarding")
public class OnboardingController {
    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @Operation(summary = "온보딩 저장",
            description = "학교 정보·내 정보·기숙사·카테고리 순위를 한 번에 저장한다. 첫 설문 제출 후에는 409.")
    @PutMapping
    public ApiResponse.Success<OnboardingResponse> save(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody OnboardingRequest request) {
        return ApiResponse.success(SuccessCode.ONBOARDING_SAVE_SUCCESS, onboardingService.save(userId, request));
    }
}
