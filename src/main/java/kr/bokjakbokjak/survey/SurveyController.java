package kr.bokjakbokjak.survey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.bokjakbokjak.common.ApiResponse;
import kr.bokjakbokjak.common.SuccessCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "설문")
@RestController
@RequestMapping("/api/surveys")
public class SurveyController {
    private final SurveyCatalogService surveyCatalogService;

    public SurveyController(SurveyCatalogService surveyCatalogService) {
        this.surveyCatalogService = surveyCatalogService;
    }

    @Operation(summary = "현재 설문 문항 조회", description = "카테고리 5개, 주제 8개, 16문항과 선택지를 표시 순서대로 반환한다.")
    @GetMapping("/current")
    public ApiResponse.Success<SurveyCatalogResponse> getCurrentSurvey() {
        return ApiResponse.success(SuccessCode.SURVEY_SCHEMA_SUCCESS, surveyCatalogService.getCurrentSurvey());
    }
}
