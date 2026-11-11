package kr.bokjakbokjak.survey.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import kr.bokjakbokjak.survey.dto.MySurveyResponse;
import kr.bokjakbokjak.survey.dto.SurveySubmitRequest;
import kr.bokjakbokjak.survey.dto.SurveySubmitResponse;
import kr.bokjakbokjak.survey.service.MySurveyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "설문")
@RestController
@RequestMapping("/api/me/survey")
public class MySurveyController {
    private final MySurveyService mySurveyService;

    public MySurveyController(MySurveyService mySurveyService) {
        this.mySurveyService = mySurveyService;
    }

    @Operation(summary = "내 설문 응답 조회", description = "제출 전에는 revision 0과 빈 배열을 반환한다.")
    @GetMapping
    public ApiResponse.Success<MySurveyResponse> getMySurvey(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(SuccessCode.MY_SURVEY_SUCCESS, mySurveyService.getMySurvey(userId));
    }

    @Operation(summary = "설문 제출·수정", description = "카테고리 3~5개 순위와 16문항 답변 전체를 저장한다.")
    @PutMapping
    public ApiResponse.Success<SurveySubmitResponse> submit(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SurveySubmitRequest request) {
        return ApiResponse.success(SuccessCode.SURVEY_SAVE_SUCCESS, mySurveyService.submit(userId, request));
    }
}
